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

import android.webkit.JavascriptInterface

class JavascriptBridge(
    private val onLogin: () -> Unit,
    private val onLogout: () -> Unit,
    private val onSignUp: () -> Unit,
    private val onKeepDeviceAwake: (Boolean) -> Unit,
    private val onReady: () -> Unit,
    private val onDismissApp: () -> Unit,
) {

    @JavascriptInterface
    fun logIn() {
        onLogin()
    }

    @JavascriptInterface
    fun logout() {
        onLogout()
    }

    @JavascriptInterface
    fun signUp() {
        onSignUp()
    }

    @JavascriptInterface
    fun keepDeviceAwake(state: Boolean) {
        onKeepDeviceAwake(state)
    }

    @JavascriptInterface
    fun ready() {
        onReady()
    }

    @JavascriptInterface
    fun dismissApp() {
        onDismissApp()
    }

    companion object {
        const val NAME = "euria"
    }
}
