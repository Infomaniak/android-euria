/*
 * Infomaniak Euria - Android
 * Copyright (C) 2025 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.infomaniak.euria.upload

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.WebView
import com.infomaniak.core.cancellable
import com.infomaniak.core.network.utils.await
import com.infomaniak.core.network.utils.bodyAsStringOrNull
import com.infomaniak.euria.data.api.ApiRoutes
import com.infomaniak.euria.data.models.js.FileInfo
import com.infomaniak.euria.data.models.js.FileUploadErrorJsResponse
import com.infomaniak.euria.data.models.js.FileUploadSucceedJsResponse
import com.infomaniak.euria.data.models.remote.FileUploadResult
import com.infomaniak.euria.di.IoDispatcher
import com.infomaniak.euria.di.MainDispatcher
import com.infomaniak.euria.utils.AccountUtils.requestCurrentUser
import com.infomaniak.euria.utils.OkHttpClientProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class UploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) {
    private val uploadJobs = mutableMapOf<String, Job>()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun uploadBitmap(webView: WebView?, bitmap: Bitmap) {
        withValidOrganizationId(webView) { organizationId ->
            val tasks = createUploadTasks(bitmap)
            uploadAttachments(webView, tasks, organizationId)
        }
    }

    suspend fun uploadFiles(webView: WebView?, uris: List<Uri>) {
        withValidOrganizationId(webView) { organizationId ->
            val tasks = createUploadTasks(uris)
            uploadAttachments(webView, tasks, organizationId)
        }
    }

    private suspend fun uploadAttachments(
        webView: WebView?,
        tasks: List<UploadTask>,
        organizationId: String
    ) {
        val currentUser = requestCurrentUser() ?: return

        val allFilesInfo = tasks.map { it.fileInfo }
        val acceptedFilesInfo = prepareFilesForUpload(webView, allFilesInfo)
        if (acceptedFilesInfo.isEmpty()) return

        val acceptedFileIds = acceptedFilesInfo.map { it.localId }.toSet()
        val validTasks = tasks.filter { it.fileInfo.localId in acceptedFileIds }

        val okHttpClient = getHttpClient(currentUser.apiToken.accessToken)

        coroutineScope {
            // We want to limit parallel uploads in case big files are selected for two reasons:
            // 1. Because we load the entire file in memory (for now, for simpler JS interop)
            // 2. We favor quicker WebPage files preview over uploads speed
            val semaphore = Semaphore(2)
            validTasks.forEach { task ->
                uploadJobs[task.fileInfo.localId] = launch {
                    semaphore.withPermit {
                        uploadAttachment(
                            byteArray = task.data,
                            okHttpClient = okHttpClient,
                            fileInfo = task.fileInfo,
                            organizationId = organizationId,
                            jsonParser = jsonParser,
                            webView = webView,
                        )
                    }
                }
            }
        }
        uploadJobs.clear()
    }

    fun cancelUpload(localId: String) {
        uploadJobs[localId]?.cancel()
    }

    private suspend fun uploadAttachment(
        byteArray: ByteArray,
        okHttpClient: OkHttpClient,
        fileInfo: FileInfo,
        organizationId: String,
        jsonParser: Json,
        webView: WebView?,
    ) {
        runCatching {
            val uploadFileResponse = uploadFile(
                info = fileInfo,
                byteArray = byteArray,
                organizationId = organizationId,
                okHttpClient = okHttpClient,
            )

            sendUploadResultToWebView(
                result = uploadFileResponse,
                jsonParser = jsonParser,
                fileInfo = fileInfo,
                webView = webView,
            )
        }.cancellable().onFailure {
            sendFileUploadError(fileInfo, "", webView, jsonParser)
        }
    }


    private suspend fun sendUploadResultToWebView(
        result: Response,
        jsonParser: Json,
        fileInfo: FileInfo,
        webView: WebView?,
    ) {
        val bodyResult = result.bodyAsStringOrNull()
        if (result.isSuccessful && bodyResult != null) {
            sendFileUploadDone(jsonParser, bodyResult, fileInfo, webView)
        } else {
            sendFileUploadError(fileInfo, bodyResult, webView, jsonParser)
        }
    }

    private suspend fun sendFileUploadDone(
        jsonParser: Json,
        bodyResult: String,
        fileInfo: FileInfo,
        webView: WebView?
    ) {
        val fileUploadResult = jsonParser.decodeFromString<FileUploadResult>(bodyResult)
        val fileUploadJsResponse = FileUploadSucceedJsResponse(
            localId = fileInfo.localId,
            remoteId = fileUploadResult.fileUploadDetailResult.remoteId,
            fileName = fileUploadResult.fileUploadDetailResult.fileName,
            type = fileUploadResult.fileUploadDetailResult.type,
        )
        withContext(mainDispatcher) {
            webView?.executeJSFunction(
                "fileUploadDone(${jsonParser.encodeToString(fileUploadJsResponse)})"
            )
        }
    }

    private suspend fun sendFileUploadError(
        fileInfo: FileInfo,
        bodyResult: String?,
        webView: WebView?,
        jsonParser: Json
    ) {
        val fileUploadErrorJsResponse =
            FileUploadErrorJsResponse(fileInfo.localId, bodyResult ?: "")
        withContext(mainDispatcher) {
            webView?.executeJSFunction(
                "fileUploadError(${jsonParser.encodeToString(fileUploadErrorJsResponse)})"
            )
        }
    }

    private suspend fun uploadFile(
        info: FileInfo,
        byteArray: ByteArray,
        organizationId: String,
        okHttpClient: OkHttpClient,
    ): Response {
        val form =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    name = "file",
                    filename = info.fileName,
                    body = byteArray.toRequestBody(info.type?.toMediaTypeOrNull()),
                )
                .build()

        val request = getUploadFileRequest(organizationId, form)
        val uploadFileCall = okHttpClient.newCall(request)
        return uploadFileCall.await()
    }

    private fun getHttpClient(apiToken: String): OkHttpClient {
        return OkHttpClientProvider.getOkHttpClientProvider(apiToken)
            .newBuilder()
            .writeTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .build()
    }

    private fun getUploadFileRequest(organizationId: String, form: MultipartBody): Request = Request
        .Builder()
        .url(ApiRoutes.uploadFile(organizationId))
        .post(form)
        .build()

    private suspend fun prepareFilesForUpload(webView: WebView?, filesInfo: List<FileInfo>): List<FileInfo> {
        var filteredFilesInfo = filesInfo
        withContext(mainDispatcher) {
            val validFilesUUIDString =
                webView?.executeJSFunction("prepareFilesForUpload(${Json.encodeToString(filteredFilesInfo)})")
            val validFilesUUID = Json.decodeFromString<List<String>>(validFilesUUIDString ?: "")
            filteredFilesInfo = filteredFilesInfo.filter { it.localId in validFilesUUID }
        }
        return filteredFilesInfo
    }

    private suspend fun WebView.executeJSFunction(functionName: String) = suspendCancellableCoroutine { continuation ->
        if (continuation.isCancelled) return@suspendCancellableCoroutine

        evaluateJavascript(functionName) {
            continuation.resume(it)
        }
    }

    private fun getFileInfo(uri: Uri): FileInfo {
        var fileName: String? = null
        var fileSize: Long? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }
        val type = context.contentResolver.getType(uri)

        return FileInfo(
            localId = UUID.randomUUID().toString(),
            fileName = fileName,
            fileSize = fileSize,
            type = type,
            uri = uri,
        )
    }

    private fun getFileInfo(bitmap: Bitmap): FileInfo {
        val date = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        return FileInfo(
            localId = UUID.randomUUID().toString(),
            fileName = "$date.jpeg",
            fileSize = bitmap.byteCount.toLong(),
            type = PHOTO_CAMERA_TYPE,
        )
    }

    private suspend fun withValidOrganizationId(
        webView: WebView?,
        validOrganizationCallback: suspend (String) -> Unit,
    ) {
        val organizationId = withContext(mainDispatcher) {
            webView?.executeJSFunction("getCurrentOrganizationId()")
        }
        // 0 or null means we're not connected so we don't want to proceed with the files
        // TODO Remove organizationId == "null" when the webPage handles this case properly
        if (organizationId == null || organizationId == "null" || organizationId == "0") return

        validOrganizationCallback(organizationId)
    }

    private suspend fun createUploadTasks(uris: List<Uri>): List<UploadTask> = withContext(ioDispatcher) {
        uris.mapNotNull { uri ->
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val byteArray = inputStream.readBytes()
                UploadTask(getFileInfo(uri), byteArray)
            }
        }
    }

    private suspend fun createUploadTasks(bitmap: Bitmap): List<UploadTask> = withContext(ioDispatcher) {
        val byteArray = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.toByteArray()
        }
        listOf(UploadTask(getFileInfo(bitmap), byteArray))
    }

    private class UploadTask(val fileInfo: FileInfo, val data: ByteArray)

    companion object {
        private const val PHOTO_CAMERA_TYPE = "image/jpeg"
    }
}
