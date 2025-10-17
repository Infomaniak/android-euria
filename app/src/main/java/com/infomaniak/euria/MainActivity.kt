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
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.infomaniak.core.compose.basics.CallableState
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.network.ApiEnvironment
import com.infomaniak.core.network.LOGIN_ENDPOINT_URL
import com.infomaniak.core.network.NetworkConfiguration
import com.infomaniak.core.network.networking.DefaultHttpClientProvider
import com.infomaniak.core.sentry.SentryLog
import com.infomaniak.euria.ui.login.CrossAppLoginViewModel
import com.infomaniak.euria.ui.login.components.OnboardingScreen
import com.infomaniak.euria.ui.theme.EuriaTheme
import com.infomaniak.lib.login.InfomaniakLogin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import splitties.coroutines.repeatWhileActive
import splitties.experimental.ExperimentalSplittiesApi

@OptIn(ExperimentalSplittiesApi::class)
class MainActivity : ComponentActivity() {

    private val crossAppLoginViewModel: CrossAppLoginViewModel by viewModels()

    private var isLoginButtonLoading by mutableStateOf(false)
    private var isSignUpButtonLoading by mutableStateOf(false)
    private var token by mutableStateOf<String?>(null)

    private val loginRequest = CallableState<List<ExternalAccount>>()
    private val infomaniakLogin: InfomaniakLogin by lazy { getInfomaniakLogin() }

    private val webViewLoginResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            with(result) {
                if (resultCode == RESULT_OK) {
                    val authCode = data?.extras?.getString(InfomaniakLogin.CODE_TAG)
                    val translatedError =
                        data?.extras?.getString(InfomaniakLogin.ERROR_TRANSLATED_TAG)
                    when {
                        translatedError?.isNotBlank() == true -> showError(translatedError)
                        authCode?.isNotBlank() == true -> authenticateUser(authCode)
                        else -> showError(getString(R.string.anErrorHasOccurred))
                    }
                } else {
                    isLoginButtonLoading = false
                    isSignUpButtonLoading = false
                }
            }
        }

    private val createAccountResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            result.handleCreateAccountActivityResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(true)

        // New modules configuration
        NetworkConfiguration.init(
            appId = BuildConfig.APPLICATION_ID,
            appVersionCode = BuildConfig.VERSION_CODE,
            appVersionName = BuildConfig.VERSION_NAME,
            apiEnvironment = ApiEnvironment.PreProd,
        )

        enableEdgeToEdge()
        if (SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        setContent {
            EuriaTheme {
                val accounts by crossAppLoginViewModel.availableAccounts.collectAsStateWithLifecycle()
                val skippedIds by crossAppLoginViewModel.skippedAccountIds.collectAsStateWithLifecycle()

                EuriaTheme {
                    Surface {
                        if (token == null) {
                            OnboardingScreen(
                                accounts = { accounts },
                                skippedIds = { skippedIds },
                                isLoginButtonLoading = { loginRequest.isAwaitingCall.not() || isLoginButtonLoading },
                                isSignUpButtonLoading = { isSignUpButtonLoading },
                                onLoginRequest = { accounts -> loginRequest(accounts) },
                                onCreateAccount = { openAccountCreationWebView() },
                                onSaveSkippedAccounts = {
                                    crossAppLoginViewModel.skippedAccountIds.value = it
                                },
                            )
                        } else {
                            WebView(
                                url = EURIA_MAIN_URL,
                                headersString = Json.encodeToString(mapOf("Authorization" to "Bearer $token")),
                                onUrlToQuitReached = {},
                                urlToQuit = "",
                            )
                        }
                    }
                }
            }
            initCrossLogin()
        }
    }

    fun Context.getInfomaniakLogin() = InfomaniakLogin(
        context = this,
        loginUrl = "${LOGIN_ENDPOINT_URL}/",
        appUID = BuildConfig.APPLICATION_ID,
        clientID = BuildConfig.CLIENT_ID,
        accessType = null,
        sentryCallback = { error -> SentryLog.e(tag = "WebViewLogin", error) }
    )

    private fun initCrossLogin() = lifecycleScope.launch {
        launch { crossAppLoginViewModel.activateUpdates(this@MainActivity) }
        launch { handleLogin(loginRequest) }
    }

    private suspend fun handleLogin(loginRequest: CallableState<List<ExternalAccount>>): Nothing =
        repeatWhileActive {
            val accountsToLogin = loginRequest.awaitOneCall()
            if (accountsToLogin.isEmpty()) openLoginWebView()
            else connectAccounts(selectedAccounts = accountsToLogin)
        }

    private suspend fun connectAccounts(selectedAccounts: List<ExternalAccount>) {
        val loginResult = crossAppLoginViewModel.attemptLogin(selectedAccounts)

        with(loginResult) {
            tokens.forEachIndexed { index, token -> }

            errorMessageIds.forEach { errorId -> showError(getString(errorId)) }
        }
    }

    private fun openLoginWebView() {
        isLoginButtonLoading = true
        infomaniakLogin.startWebViewLogin(webViewLoginResultLauncher)
    }

    private fun authenticateUser(authCode: String) {
        lifecycleScope.launch {
            runCatching {
                val tokenResult = infomaniakLogin.getToken(
                    okHttpClient = DefaultHttpClientProvider.okHttpClient,
                    code = authCode,
                )

                when (tokenResult) {
                    is InfomaniakLogin.TokenResult.Success -> {
                        token = tokenResult.apiToken.accessToken //TODO Save this token in SharedPreferences
                    }

                    is InfomaniakLogin.TokenResult.Error -> {
                        showError(
                            getLoginErrorDescription(
                                this@MainActivity,
                                tokenResult.errorStatus
                            )
                        )
                    }
                }
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception
                SentryLog.e(TAG, "Failure on getToken", exception)
            }
        }
    }

    private fun showError(error: String) {
        Snackbar.make(
            window.decorView.findViewById(android.R.id.content),
            error,
            Snackbar.LENGTH_LONG
        ).show()
        isLoginButtonLoading = false
        isSignUpButtonLoading = false
    }

    private fun openAccountCreationWebView() {
        isSignUpButtonLoading = true
        startAccountCreation()
    }

    private fun startAccountCreation() {
        infomaniakLogin.startCreateAccountWebView(
            resultLauncher = createAccountResultLauncher,
            createAccountUrl = CREATE_ACCOUNT_URL,
            successHost = CREATE_ACCOUNT_SUCCESS_HOST,
            cancelHost = CREATE_ACCOUNT_CANCEL_HOST,
        )
    }

    private fun ActivityResult.handleCreateAccountActivityResult() {
        if (resultCode == RESULT_OK) {
            val translatedError = data?.getStringExtra(InfomaniakLogin.ERROR_TRANSLATED_TAG)
            when {
                translatedError.isNullOrBlank() -> infomaniakLogin.startWebViewLogin(
                    webViewLoginResultLauncher,
                    false
                )

                else -> showError(translatedError)
            }
        } else {
            isSignUpButtonLoading = false
        }
    }

    companion object {
        const val TAG = "MainActivity"

        fun getLoginErrorDescription(
            context: Context,
            error: InfomaniakLogin.ErrorStatus
        ): String {
            return context.getString(
                when (error) {
                    InfomaniakLogin.ErrorStatus.SERVER -> R.string.serverError
                    InfomaniakLogin.ErrorStatus.CONNECTION -> R.string.connectionError
                    else -> R.string.anErrorHasOccurred
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EuriaTheme {
    }
}
