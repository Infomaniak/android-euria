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
package com.infomaniak.euria.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import com.infomaniak.core.common.dynamicLazyMap
import com.infomaniak.core.common.extensions.goToAppStore
import com.infomaniak.euria.BuildConfig
import com.infomaniak.euria.EURIA_MAIN_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class CustomWebViewClient(
    private val scope: CoroutineScope,
    private val onPageSuccessfullyLoaded: (WebView) -> Unit,
    private val onDownloadRequest: (url: String) -> Unit = {},
) : WebViewClient() {

    private var hasReceivedError = false

    private val urlDownloadTriggers = scope.dynamicLazyMap(cacheManager = { _, _ -> awaitCancellation() }) { url: String ->
        Channel<Unit>(capacity = Channel.CONFLATED).also { triggerDlEvents ->
            if (url.isEmpty()) return@also

            triggerDlEvents.receiveAsFlow()
                .onEach {
                    withContext(Dispatchers.Main) {
                        onDownloadRequest(url)
                    }
                    delay(1_500.toDuration(DurationUnit.MILLISECONDS))
                }.launchIn(scope)
        }
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        if (BuildConfig.DEBUG) handler.proceed()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        val host = request.url.host
        val euriaHost = EURIA_MAIN_URL.toHttpUrl().host
        val path = request.url.encodedPath.orEmpty()

        if (url.startsWith("intent://")) {
            return handleIntentUri(view, url)
        }

        if (host == euriaHost && path.endsWith("/download")) {
            triggerDownloadOnce(url)
            return true
        }

        return when (host) {
            euriaHost -> false
            else -> {
                openExternalBrowser(view, request)
                true
            }
        }
    }

    private fun handleIntentUri(view: WebView, intentUrl: String): Boolean {
        return runCatching {
            val intent = Intent.parseUri(intentUrl, Intent.URI_INTENT_SCHEME)
            val packageName = intent.`package`
            val fallbackUrl = intent.getStringExtra("browser_fallback_url")

            when {
                !packageName.isNullOrBlank() -> {
                    view.context.goToAppStore(packageName)
                    true
                }
                !fallbackUrl.isNullOrBlank() -> {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUrl.toUri()))
                    true
                }
                else -> false
            }
        }.getOrElse { false }
    }

    private fun triggerDownloadOnce(url: String) {
        urlDownloadTriggers.useElement(url) { it.trySend(Unit) }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (!hasReceivedError) onPageSuccessfullyLoaded(view)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) hasReceivedError = true
    }

    private fun openExternalBrowser(view: WebView, request: WebResourceRequest) {
        runCatching {
            view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
        }
    }
}
