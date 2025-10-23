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

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.infomaniak.core.compose.basics.CallableState
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.network.LOGIN_ENDPOINT_URL
import com.infomaniak.core.network.networking.DefaultHttpClientProvider
import com.infomaniak.core.sentry.SentryLog
import com.infomaniak.euria.MainActivity.Companion.TAG
import com.infomaniak.euria.MainActivity.Companion.getLoginErrorDescription
import com.infomaniak.euria.data.UserSharedPref.getToken
import com.infomaniak.euria.data.UserSharedPref.saveToken
import com.infomaniak.euria.data.UserSharedPref.saveUserId
import com.infomaniak.lib.login.ApiToken
import com.infomaniak.lib.login.InfomaniakLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import splitties.coroutines.repeatWhileActive
import splitties.experimental.ExperimentalSplittiesApi
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    val showSplashScreen = MutableStateFlow(true)

    private val context by lazy { getApplication<Application>() }
    val infomaniakLogin: InfomaniakLogin by lazy { context.getInfomaniakLogin() }

    var token by mutableStateOf(context.getToken())
    var launchMediaChooser by mutableStateOf(false)

    init {
        viewModelScope.launch {
            delay(DELAY_SPLASHSCREEN)
            showSplashScreen.emit(false)
        }
    }

    fun Context.getInfomaniakLogin() = InfomaniakLogin(
        context = this,
        loginUrl = "${LOGIN_ENDPOINT_URL}/",
        appUID = BuildConfig.APPLICATION_ID,
        clientID = BuildConfig.CLIENT_ID,
        accessType = null,
        sentryCallback = { error -> SentryLog.e(tag = "WebViewLogin", error) },
    )

    fun authenticateUser(authCode: String, showError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val tokenResult = infomaniakLogin.getToken(
                    okHttpClient = DefaultHttpClientProvider.okHttpClient,
                    code = authCode,
                )

                when (tokenResult) {
                    is InfomaniakLogin.TokenResult.Success -> {
                        saveUserInfo(tokenResult.apiToken)
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

    fun saveUserInfo(apiToken: ApiToken) {
        with(context) {
            token = apiToken.accessToken
            saveToken(apiToken.accessToken)
            saveUserId(apiToken.userId)
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

    companion object {
        private const val DELAY_SPLASHSCREEN = 2_000L
    }
}
