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
package com.infomaniak.euria.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.infomaniak.euria.BuildConfig

class CustomWebViewClient(
    private val onPageSucessfullyLoaded: () -> Unit,
    private val onPageFailedToLoad: () -> Unit
) : WebViewClient() {

    private var hasReceivedError = false

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // In order to use localhost, we have to ignore all SSL errors and proceed
        if (BuildConfig.DEBUG) handler.proceed()
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        super.onReceivedHttpError(view, request, errorResponse)

        if (request.url.path?.endsWith("users/me") == true && errorResponse.statusCode >= 401) {
            onPageFailedToLoad()
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        runCatching {
            view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
        }
        return true
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (!hasReceivedError) onPageSucessfullyLoaded()
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) hasReceivedError = true
    }
}
