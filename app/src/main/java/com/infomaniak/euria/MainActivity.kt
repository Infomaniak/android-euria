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
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.ConfigurationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.infomaniak.core.compose.basics.CallableState
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.network.ApiEnvironment
import com.infomaniak.core.network.NetworkConfiguration
import com.infomaniak.core.observe
import com.infomaniak.core.webview.ui.components.WebView
import com.infomaniak.euria.data.UserSharedPref
import com.infomaniak.euria.ui.login.CrossAppLoginViewModel
import com.infomaniak.euria.ui.login.components.OnboardingScreen
import com.infomaniak.euria.ui.theme.EuriaTheme
import com.infomaniak.euria.ui.theme.LocalCustomColorScheme
import com.infomaniak.euria.webview.CustomWebChromeClient
import com.infomaniak.euria.webview.CustomWebViewClient
import com.infomaniak.euria.webview.JavascriptBridge
import com.infomaniak.lib.login.InfomaniakLogin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import splitties.experimental.ExperimentalSplittiesApi
import javax.inject.Inject
import com.infomaniak.core.R as RCore

@AndroidEntryPoint
@OptIn(ExperimentalSplittiesApi::class)
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var twoFactorAuthManager: TwoFactorAuthManager

    @Inject
    lateinit var userSharedPref: UserSharedPref

    private val mainViewModel: MainViewModel by viewModels()
    private val crossAppLoginViewModel: CrossAppLoginViewModel by viewModels()

    private var isLoginButtonLoading by mutableStateOf(false)
    private var isSignUpButtonLoading by mutableStateOf(false)

    private val cookieManager by lazy { CookieManager.getInstance() }
    private val jsBridge by lazy {
        JavascriptBridge(
            onLogout = {
                cookieManager.removeAllCookies(null)
                mainViewModel.logout()
            },
        )
    }

    private val loginRequest = CallableState<List<ExternalAccount>>()

    private val webViewLoginResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            with(result) {
                if (resultCode == RESULT_OK) {
                    val authCode = data?.extras?.getString(InfomaniakLogin.CODE_TAG)
                    val translatedError =
                        data?.extras?.getString(InfomaniakLogin.ERROR_TRANSLATED_TAG)
                    when {
                        translatedError?.isNotBlank() == true -> showError(translatedError)
                        authCode?.isNotBlank() == true -> mainViewModel.authenticateUser(authCode) { showError(it) }
                        else -> showError(getString(RCore.string.anErrorHasOccurred))
                    }
                }

                isLoginButtonLoading = false
                isSignUpButtonLoading = false
            }
        }

    private var filePathCallback: ValueCallback<Array<out Uri>>? = null

    private val createAccountResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            result.handleCreateAccountActivityResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val installSplashScreen = installSplashScreen()

        installSplashScreen.setKeepOnScreenCondition { true }
        mainViewModel.showSplashScreen.observe(this) { showSplashScreen ->
            installSplashScreen.setKeepOnScreenCondition { showSplashScreen }
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

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
                val token by mainViewModel.token.collectAsStateWithLifecycle()

                Surface {
                    if (token == null) {
                        OnboardingScreen(
                            accounts = { accounts },
                            skippedIds = { skippedIds },
                            isLoginButtonLoading = { loginRequest.isAwaitingCall.not() || isLoginButtonLoading },
                            isSignUpButtonLoading = { isSignUpButtonLoading },
                            onLoginRequest = { accounts -> loginRequest(accounts) },
                            onCreateAccount = { openAccountCreationWebView() },
                            onSaveSkippedAccounts = { crossAppLoginViewModel.skippedAccountIds.value = it },
                        )
                    } else {
                        EuriaMainScreen(token)
                    }

                    TwoFactorAuthApprovalAutoManagedBottomSheet(twoFactorAuthManager)
                }
            }
            startCrossAppLoginService()
            initCrossLogin()
        }
    }

    @Composable
    private fun EuriaMainScreen(token: String?) {
        setTokenToCookie(token)

        ShowFileChooser()

        WebView(
            url = EURIA_MAIN_URL,
            domStorageEnabled = true,
            systemBarsColor = LocalCustomColorScheme.current.systemBarsColor,
            webViewClient = CustomWebViewClient(),
            webChromeClient = getCustomWebChromeClient(),
            callback = { webview -> webview.addJavascriptInterface(jsBridge, JavascriptBridge.NAME) },
        )
    }

    private fun setTokenToCookie(token: String?) {
        val currentLocale = ConfigurationCompat.getLocales(resources.configuration).get(0)?.toLanguageTag() ?: "en-US"
        val cookieString = "USER-TOKEN=${token}; USER-LANGUAGE=${currentLocale} path=/"
        cookieManager.setCookie(EURIA_MAIN_URL.toHttpUrl().host, cookieString)
    }

    @Composable
    private fun ShowFileChooser() {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
            onResult = { uris: List<Uri> ->
                filePathCallback?.onReceiveValue(uris.toTypedArray())
                mainViewModel.launchMediaChooser = false
            }
        )

        if (mainViewModel.launchMediaChooser) {
            launcher.launch(arrayOf("*/*"))
        }
    }

    private fun getCustomWebChromeClient(): CustomWebChromeClient {
        return CustomWebChromeClient(
            onShowFileChooser = { filePathCallback, _ ->
                this@MainActivity.filePathCallback = filePathCallback
                mainViewModel.launchMediaChooser = true
                true
            }
        )
    }

    fun startCrossAppLoginService() {
        val intent = Intent(this, CrossAppLoginService::class.java).apply {
            putExtra(CrossAppLoginService.EXTRA_USER_ID, userSharedPref.userId)
        }

        startService(intent)
    }

    private fun initCrossLogin() = lifecycleScope.launch {
        launch { crossAppLoginViewModel.activateUpdates(this@MainActivity) }
        launch {
            mainViewModel.handleLogin(
                loginRequest,
                openLoginWebView = { openLoginWebView() },
                attemptLogin = {
                    val apiToken = crossAppLoginViewModel.attemptLogin(it).tokens[0]
                    mainViewModel.saveUserInfo(apiToken) {
                        showError(it)
                    }
                },
            )
        }
    }

    private fun openLoginWebView() {
        isLoginButtonLoading = true
        mainViewModel.infomaniakLogin.startWebViewLogin(webViewLoginResultLauncher)
    }

    private fun showError(error: String) {
        Snackbar.make(
            window.decorView.findViewById(android.R.id.content),
            error,
            Snackbar.LENGTH_LONG,
        ).show()
        isLoginButtonLoading = false
        isSignUpButtonLoading = false
    }

    private fun openAccountCreationWebView() {
        isSignUpButtonLoading = true
        startAccountCreation()
    }

    private fun startAccountCreation() {
        mainViewModel.infomaniakLogin.startCreateAccountWebView(
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
                translatedError.isNullOrBlank() -> {
                    mainViewModel.infomaniakLogin.startWebViewLogin(webViewLoginResultLauncher, removeCookies = false)
                }
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
                    else -> RCore.string.anErrorHasOccurred
                }
            )
        }
    }
}
