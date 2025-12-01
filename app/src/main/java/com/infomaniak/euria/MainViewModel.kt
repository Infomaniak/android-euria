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
package com.infomaniak.euria

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infomaniak.core.auth.extensions.logoutToken
import com.infomaniak.core.auth.models.user.User
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.network.NetworkAvailability
import com.infomaniak.core.network.networking.DefaultHttpClientProvider
import com.infomaniak.core.network.utils.await
import com.infomaniak.core.network.utils.bodyAsStringOrNull
import com.infomaniak.core.sentry.SentryLog
import com.infomaniak.core.ui.compose.basics.CallableState
import com.infomaniak.euria.MainActivity.Companion.TAG
import com.infomaniak.euria.MainActivity.Companion.getLoginErrorDescription
import com.infomaniak.euria.data.LocalSettings
import com.infomaniak.euria.data.api.ApiRoutes
import com.infomaniak.euria.network.ApiRepository
import com.infomaniak.euria.utils.AccountUtils
import com.infomaniak.euria.utils.AccountUtils.requestCurrentUser
import com.infomaniak.euria.utils.OkHttpClientProvider
import com.infomaniak.euria.utils.extensions.getInfomaniakLogin
import com.infomaniak.lib.login.ApiToken
import com.infomaniak.lib.login.InfomaniakLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import splitties.coroutines.repeatWhileActive
import splitties.experimental.ExperimentalSplittiesApi
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val localSettings: LocalSettings,
) : ViewModel() {

    val infomaniakLogin: InfomaniakLogin by lazy { context.getInfomaniakLogin() }
    val cookieManager: CookieManager by lazy { CookieManager.getInstance() }

    val isNetworkAvailable = NetworkAvailability(context).isNetworkAvailable.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val isWebAppReady = MutableStateFlow(false)
    val webViewQueries = Channel<String>(capacity = Channel.CONFLATED)
    val userState: StateFlow<UserState> = AccountUtils.getCurrentUserFlow().map {
        if (it == null) {
            UserState.NotLoggedIn
        } else {
            UserState.LoggedIn(it)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, UserState.Loading)

    var skipOnboarding by mutableStateOf(localSettings.skipOnboarding)
    var launchMediaChooser by mutableStateOf(false)
    var hasSeenWebView by mutableStateOf(false)
    var microphonePermissionRequest by mutableStateOf<PermissionRequest?>(null)

    var filesToShare = MutableStateFlow(emptyList<Uri>())

    fun authenticateUser(authCode: String, forceRefreshWebView: () -> Unit, showError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val tokenResult = infomaniakLogin.getToken(
                    okHttpClient = DefaultHttpClientProvider.okHttpClient,
                    code = authCode,
                )

                when (tokenResult) {
                    is InfomaniakLogin.TokenResult.Success -> {
                        saveUserInfo(tokenResult.apiToken, showError)

                        // We only want to refresh the WebView if we logged in inside the WebView, in Euria Free mode
                        if (localSettings.skipOnboarding) forceRefreshWebView()

                        skipOnboarding(false)
                    }
                    is InfomaniakLogin.TokenResult.Error -> {
                        showError(getLoginErrorDescription(context, tokenResult.errorStatus))
                    }
                }
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception
                SentryLog.e(TAG, "Failure on getToken", exception)
            }
        }
    }

    fun saveUserInfo(apiToken: ApiToken, showError: (String) -> Unit) {
        viewModelScope.launch {
            ApiRepository.getUserProfile(OkHttpClientProvider.getOkHttpClientProvider(apiToken.accessToken)).data?.let {
                it.apiToken = apiToken
                it.organizations = arrayListOf()
                AccountUtils.addUser(it)
            } ?: run {
                showError(context.getString(R.string.connectionError))
            }
        }
    }

    @OptIn(ExperimentalSplittiesApi::class)
    suspend fun handleLogin(
        loginRequest: CallableState<List<ExternalAccount>>,
        openLoginWebView: () -> Unit,
        attemptLogin: suspend (List<ExternalAccount>) -> Unit,
    ) {
        repeatWhileActive {
            val accountsToLogin = loginRequest.awaitOneCall()
            if (accountsToLogin.isEmpty()) openLoginWebView()
            else attemptLogin(accountsToLogin)
        }
    }

    fun logout() {
        viewModelScope.launch {
            requestCurrentUser()?.let { currentUser ->
                infomaniakLogin.logoutToken(currentUser)
                cookieManager.removeAllCookies(null)
                withContext(Dispatchers.IO) { cookieManager.flush() }
                WebStorage.getInstance().deleteAllData()
                AccountUtils.removeUser(currentUser)
                localSettings.removeSettings()
            }
        }
    }

    fun skipOnboarding(state: Boolean) {
        localSettings.skipOnboarding = state
        skipOnboarding = state
    }

    fun readFiles(webView: WebView?, uris: List<Uri>) {
        val jsonParser = Json { ignoreUnknownKeys = true }

        viewModelScope.launch {
            val organizationId = async { webView?.executeJSFunction("getCurrentOrganizationId()") }.await()
            // 0 or null means we're not connected so we don't want to proceed with the files
            if (organizationId == "null" || organizationId == "0") return@launch

            val allowedMimeTypesString = async { webView?.executeJSFunction("getAllowedFilesMimeTypes()") }.await()
            if (allowedMimeTypesString == null) return@launch
            val allowedMimeTypes = Json.decodeFromString<List<String>>(allowedMimeTypesString)

            val allowedFilesSizeString = async { webView?.executeJSFunction("getAllowedFilesSizes()") }.await()
            if (allowedFilesSizeString == null) return@launch
            val allowedFilesSize = Json.decodeFromString<AllowedFilesSizes>(allowedFilesSizeString)

            withContext(Dispatchers.IO) {
                val currentUser = requestCurrentUser()
                val allFilesInfo = mutableListOf<FileInfo>()
                val validFilesInfo = mutableListOf<FileInfo>()

                // First loop to get all files information to send it to the WebView
                uris.forEach {
                    val fileInfo = getFileInfo(it)

                    if (
                        allowedMimeTypes.contains(fileInfo.type)
                        && ((fileInfo.type?.startsWith("image") == true && fileInfo.fileSize!! <= allowedFilesSize.images)
                                || (fileInfo.type?.startsWith("image") == false && fileInfo.fileSize!! <= allowedFilesSize.files))
                    ) {
                        validFilesInfo.add(fileInfo)
                    }

                    allFilesInfo.add(fileInfo)

                    ensureActive()
                }

                // Send the `startUploadEvent` with all files in order to display the right number of elements in the WebView
                // but also to let the WebView display errors if one file is not valid
                withContext(Dispatchers.Main) {
                    async { webView?.executeJSFunction("startFilesUpload(${Json.encodeToString(allFilesInfo)})") }.await()
                }

                // If no files are valid, we can leave safely
                if (validFilesInfo.isEmpty()) return@withContext

                // Upload files
                validFilesInfo.forEach {
                    if (it.uri == null) return@forEach

                    context.contentResolver.openInputStream(it.uri)?.buffered()?.use { fileInputStream ->
                        val byteArray = fileInputStream.readBytes()

                        currentUser?.apiToken?.accessToken?.let { apiToken ->
                            val form =
                                MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("file", it.fileName, byteArray.toRequestBody())
                                    .build()

                            val request =
                                Request.Builder().url(ApiRoutes.uploadFile(organizationId.toString())).post(form).build()

                            val result =
                                OkHttpClientProvider.getOkHttpClientProvider(apiToken).newCall(request).await()

                            val bodyResult = result.bodyAsStringOrNull()
                            if (result.isSuccessful && bodyResult != null) {
                                val fileUploadResult = jsonParser.decodeFromString<FileUploadResult>(bodyResult)
                                val fileUploadJsResponse = FileUploadSucceedJsResponse(
                                    localId = it.localId,
                                    remoteId = fileUploadResult.fileUploadDetailResult.remoteId,
                                    fileName = fileUploadResult.fileUploadDetailResult.fileName,
                                    type = fileUploadResult.fileUploadDetailResult.type,
                                )
                                withContext(Dispatchers.Main) {
                                    webView?.executeJSFunction("fileUploadDone(${jsonParser.encodeToString(fileUploadJsResponse)})")
                                }
                            } else {
                                val fileUploadErrorJsResponse = FileUploadErrorJsResponse(it.localId, bodyResult ?: "")
                                withContext(Dispatchers.Main) {
                                    webView?.executeJSFunction(
                                        "fileUploadError(${jsonParser.encodeToString(fileUploadErrorJsResponse)})"
                                    )
                                }
                            }
                        }
                    }

                    ensureActive()
                }
            }
        }
    }

    @Serializable
    data class AllowedFilesSizes(val files: Int, val images: Int)

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

    @Serializable
    data class FileInfo(
        @SerialName("ref") val localId: String,
        @SerialName("name") val fileName: String?,
        @SerialName("size") val fileSize: Long?,
        @SerialName("mimeType") val type: String?,
        @Transient val uri: Uri? = null,
    )

    @Serializable
    data class FileUploadResult(
        @SerialName("data") val fileUploadDetailResult: FileUploadDetailResult,
    )

    @Serializable
    data class FileUploadDetailResult(
        @SerialName("id") val remoteId: String,
        @SerialName("name") val fileName: String?,
        @SerialName("mime_type") val type: String?,
    )

    @Serializable
    data class FileUploadSucceedJsResponse(
        @SerialName("ref") val localId: String,
        @SerialName("id") val remoteId: String,
        @SerialName("name") val fileName: String?,
        @SerialName("mimeType") val type: String?,
    )

    @Serializable
    data class FileUploadErrorJsResponse(
        @SerialName("ref") val localId: String,
        @SerialName("error") val error: String,
    )

    sealed interface UserState {
        object Loading : UserState
        data class LoggedIn(val user: User) : UserState
        object NotLoggedIn : UserState
    }
}
