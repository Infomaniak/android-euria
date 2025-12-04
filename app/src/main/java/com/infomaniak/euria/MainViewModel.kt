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
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebStorage
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
import com.infomaniak.core.sentry.SentryLog
import com.infomaniak.core.ui.compose.basics.CallableState
import com.infomaniak.euria.MainActivity.Companion.TAG
import com.infomaniak.euria.MainActivity.Companion.getLoginErrorDescription
import com.infomaniak.euria.data.LocalSettings
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.coroutines.repeatWhileActive
import splitties.experimental.ExperimentalSplittiesApi
import javax.inject.Inject

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

    sealed interface UserState {
        object Loading : UserState
        data class LoggedIn(val user: User) : UserState
        object NotLoggedIn : UserState
    }
}
