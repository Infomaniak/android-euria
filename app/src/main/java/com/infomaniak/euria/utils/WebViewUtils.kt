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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.infomaniak.core.ui.view.toDp
import com.infomaniak.euria.EURIA_MAIN_URL
import com.infomaniak.euria.MainActivity.Companion.EXTRA_QUERY
import com.infomaniak.euria.webview.CustomWebChromeClient
import com.infomaniak.euria.webview.JavascriptBridge
import okhttp3.HttpUrl.Companion.toHttpUrl

class WebViewUtils(
    private val context: Context,
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
