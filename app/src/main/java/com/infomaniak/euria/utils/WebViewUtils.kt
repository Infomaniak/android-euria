/*
 * Infomaniak Euria - Android
 * Copyright (C) 2025-2026 Infomaniak Network SA
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

package com.infomaniak.euria.utils

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Build.VERSION.SDK_INT
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.infomaniak.core.network.utils.await
import com.infomaniak.core.ui.view.utils.toDp
import com.infomaniak.euria.EURIA_MAIN_URL
import com.infomaniak.euria.MainActivity.Companion.EXTRA_QUERY
import com.infomaniak.euria.webview.CustomWebChromeClient
import com.infomaniak.euria.webview.JavascriptBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class WebViewUtils(
    private val context: Context,
    private val scope: CoroutineScope,
    val javascriptBridge: JavascriptBridge,
) {

    private val cookieManager by lazy { CookieManager.getInstance() }

    var webView: WebView? = null

    fun setUserLanguageToCookie() {
        val currentLocale = ConfigurationCompat.getLocales(context.resources.configuration).get(0)?.language ?: "en"
        cookieManager.setCookie(EURIA_MAIN_URL.toHttpUrl().host, "USER-LANGUAGE=${currentLocale}")
    }

    fun setTokenToCookie(token: String?) {
        if (token != null) cookieManager.setCookie(EURIA_MAIN_URL.toHttpUrl().host, "USER-TOKEN=${token}")
    }

    fun applySafeAreaInsets(
        webView: WebView,
        insets: WindowInsets,
        density: Density,
        layoutDirection: LayoutDirection,
    ) {
        val top = insets.getTop(density).toDp(webView)
        val right = insets.getRight(density, layoutDirection).toDp(webView)
        val bottom = insets.getBottom(density).toDp(webView)
        val left = insets.getLeft(density, layoutDirection).toDp(webView)

        val script = """
        (function() {
            var styleTag = document.getElementById("$CSS_TAG_ID");
            
            if (!styleTag) {
                styleTag = document.createElement("style");
                styleTag.id = "$CSS_TAG_ID";
                document.head.appendChild(styleTag);
            }
            
            styleTag.textContent = `
                :root {
                    --safe-area-inset-top: ${top}px;
                    --safe-area-inset-left: ${left}px;
                    --safe-area-inset-right: ${right}px;
                    --safe-area-inset-bottom: ${bottom}px;
                }
            `;
        })();
    """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    fun setupKeyboardDetection(webView: WebView, insets: WindowInsets, density: Density) {
        // Ensure WebView doesn't consume insets automatically
        webView.fitsSystemWindows = false

        fun Int.toDp(): String = (this / webView.context.resources.displayMetrics.density).toInt().toString()

        fun updateViewport(insets: WindowInsetsCompat) {
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val keyboardHeight = imeInsets.bottom

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val safeBottomCss = (if (keyboardHeight > 0) keyboardHeight else systemBars.bottom).toDp()

            val screenHeightCss = webView.height.toDp()
            val visibleHeightCss = (webView.height - keyboardHeight).toDp()
            val keyboardHeightCss = keyboardHeight.toDp()
            webView.evaluateJavascript(
                """
            document.documentElement.style.setProperty('--android-screen-height', '${screenHeightCss}px');
            document.documentElement.style.setProperty('--android-viewport-height', '${visibleHeightCss}px');
            document.documentElement.style.setProperty('--android-keyboard-height', '${keyboardHeightCss}px');
            document.documentElement.style.setProperty('--android-safe-area-inset-bottom', '${safeBottomCss}px');
            """.trimIndent(), null
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(webView) { _, insets ->
            updateViewport(insets)

            insets
        }

        ViewCompat.requestApplyInsets(webView)
    }

    fun updateWebViewQueryFrom(intent: Intent, updateWebViewQuery: (String) -> Unit) {
        val query = intent.getStringExtra(EXTRA_QUERY)
            ?: getProcessedDeeplinkUrl(intent)?.let { deeplinkUrl ->
                deeplinkUrl.substringAfter(deeplinkUrl.toHttpUrl().host)
            }
            ?: return

        updateWebViewQuery(query)
    }

    private fun getProcessedDeeplinkUrl(intent: Intent): String? {
        val deeplinkUri = intent.data ?: return null
        val deeplinkPath = deeplinkUri.path ?: return null
        return when {
            deeplinkUri.host?.startsWith("euria") == true -> deeplinkUri.toString()
            else -> parseKSuiteDeeplink(deeplinkPath)
        }
    }

    private fun parseKSuiteDeeplink(deeplink: String): String {
        return when {
            deeplink.endsWith("euria") -> EURIA_MAIN_URL
            deeplink.startsWith("/all") -> "$EURIA_MAIN_URL/${deeplink.substringAfter("euria/")}"
            else -> "$EURIA_MAIN_URL/${deeplink.replace("/euria", "")}"
        }
    }

    fun startDownloadAsync(url: String, userId: Int) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val uri = url.toUri()
                val httpClient = AccountUtils.getHttpClient(userId)
                val contentDisposition = httpClient.newCall(Request.Builder().url(url).head().build())
                    .await()
                    .use { it.header("Content-Disposition") ?: "" }

                val filename = extractFilename(contentDisposition) ?: uri.lastPathSegment ?: "download"

                val request = DownloadManager.Request(uri).apply {
                    addRequestHeader("Authorization", "Bearer ${AccountUtils.requestCurrentUser()?.apiToken?.accessToken}")

                    setTitle(filename)
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename)
                    setNotificationVisibility(
                        if (SDK_INT >= 37) {
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION
                        } else {
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        }
                    )
                }

                (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            }
        }
    }

    private fun extractFilename(contentDisposition: String): String? {
        return when {
            contentDisposition.contains("filename=") -> {
                val start = contentDisposition.indexOf("filename=") + 9
                val end = contentDisposition.indexOf('"', start + 1).takeIf { it > 0 } ?: contentDisposition.length
                contentDisposition.substring(start, end).trim('"', ' ')
            }
            else -> null
        }
    }

    fun getCustomWebChromeClient(
        filePathCallback: (ValueCallback<Array<out Uri>>) -> Unit,
        launchMediaChooser: (Boolean) -> Unit,
        microphonePermissionRequest: (PermissionRequest?) -> Unit,
    ): CustomWebChromeClient {
        return CustomWebChromeClient(
            onShowFileChooser = { filePathCallback, _ ->
                filePathCallback(filePathCallback)
                launchMediaChooser(true)
                true
            },
            onRequestMicrophonePermission = { permissionRequest ->
                val hasRecordAudioPermission = hasPermission(Manifest.permission.RECORD_AUDIO)
                val hasModifyAudioPermission = hasPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)

                if (hasRecordAudioPermission && hasModifyAudioPermission) {
                    permissionRequest.grant(permissionRequest.resources)
                    microphonePermissionRequest(null)
                } else {
                    microphonePermissionRequest(permissionRequest)
                }
            },
        )
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        // This tag is named like that just to have a unique identifier but the Web page does not rely on it
        const val CSS_TAG_ID = "mobile-inset-style"
    }
}
