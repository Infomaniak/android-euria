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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.material.snackbar.Snackbar
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.observe
import com.infomaniak.core.twofactorauth.back.TwoFactorAuthManager
import com.infomaniak.core.twofactorauth.front.TwoFactorAuthApprovalAutoManagedBottomSheet
import com.infomaniak.core.ui.compose.basics.CallableState
import com.infomaniak.core.ui.view.toDp
import com.infomaniak.core.webview.ui.components.WebView
import com.infomaniak.euria.MainViewModel.UserState
import com.infomaniak.euria.ui.login.CrossAppLoginViewModel
import com.infomaniak.euria.ui.login.components.OnboardingScreen
import com.infomaniak.euria.ui.noNetwork.NoNetworkScreen
import com.infomaniak.euria.ui.theme.EuriaTheme
import com.infomaniak.euria.utils.AccountUtils
import com.infomaniak.euria.webview.CustomWebChromeClient
import com.infomaniak.euria.webview.CustomWebViewClient
import com.infomaniak.euria.webview.JavascriptBridge
import com.infomaniak.lib.login.InfomaniakLogin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import splitties.experimental.ExperimentalSplittiesApi
import com.infomaniak.core.R as RCore

val twoFactorAuthManager = TwoFactorAuthManager { userId -> AccountUtils.getHttpClient(userId) }

@AndroidEntryPoint
@OptIn(ExperimentalSplittiesApi::class)
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val crossAppLoginViewModel: CrossAppLoginViewModel by viewModels()

    private var isLoginButtonLoading by mutableStateOf(false)
    private var isSignUpButtonLoading by mutableStateOf(false)

    private val cookieManager by lazy { CookieManager.getInstance() }
    private val jsBridge by lazy { getEuriaJavascriptBridge() }

    private var keepSplashScreen = MutableStateFlow(true)

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
        val splashScreen = installSplashScreen().apply { setKeepOnScreenCondition { true } }

        keepSplashScreen.observe(lifecycleOwner = this, state = Lifecycle.State.CREATED) {
            splashScreen.setKeepOnScreenCondition { it }
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        enableEdgeToEdge()
        if (SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        lifecycleScope.launch {
            mainViewModel.userState.collectLatest {
                if (it is UserState.NotLoggedIn) runLogin()
            }
        }

        setContent {
            EuriaTheme {
                val accountsCheckingState by crossAppLoginViewModel.accountsCheckingState.collectAsStateWithLifecycle()
                val skippedIds by crossAppLoginViewModel.skippedAccountIds.collectAsStateWithLifecycle()
                val userState by mainViewModel.userState.collectAsStateWithLifecycle()
                val isNetworkAvailable by mainViewModel.isNetworkAvailable.collectAsStateWithLifecycle()

                Surface {
                    when {
                        userState is UserState.Loading -> Unit
                        userState is UserState.NotLoggedIn -> {
                            keepSplashScreen.update { false }
                            OnboardingScreen(
                                accountsCheckingState = { accountsCheckingState },
                                skippedIds = { skippedIds },
                                isLoginButtonLoading = { loginRequest.isAwaitingCall.not() || isLoginButtonLoading },
                                isSignUpButtonLoading = { isSignUpButtonLoading },
                                onLoginRequest = { accounts -> loginRequest(accounts) },
                                onCreateAccount = { openAccountCreationWebView() },
                                onSaveSkippedAccounts = { crossAppLoginViewModel.skippedAccountIds.value = it },
                            )
                        }
                        isNetworkAvailable || mainViewModel.hasSeenWebView -> {
                            val userState = userState as UserState.LoggedIn
                            EuriaMainScreen(userState.user.apiToken.accessToken)
                        }
                        else -> {
                            keepSplashScreen.update { false }
                            NoNetworkScreen()
                        }
                    }

                    TwoFactorAuthApprovalAutoManagedBottomSheet(twoFactorAuthManager)
                }
            }
        }
    }

    private fun getEuriaJavascriptBridge() = JavascriptBridge(
        onDismissApp = { finish() },
        onLogout = { mainViewModel.logout() },
        onKeepDeviceAwake = { state ->
            if (state) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        },
    )

    private fun getProcessedDeeplinkUrl(): String? {
        val deeplinkUri = intent.data ?: return null
        val deeplinkPath = deeplinkUri.path ?: return null
        fun isEuriaDeeplink() = deeplinkUri.host?.startsWith("euria") == true
        return when {
            isEuriaDeeplink() -> deeplinkUri.toString()
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

    @Composable
    private fun EuriaMainScreen(token: String?) {
        setTokenToCookie(token)

        var currentWebview: WebView? by remember { mutableStateOf(null) }
        val insets = WindowInsets.safeDrawing
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current

        // Need this to apply the insets to the WebView when these ones changes
        applySafeAreaInsetsToWebView(currentWebview, insets, density, layoutDirection)

        AskMicrophonePermission()
        ShowFileChooser()

        HandleBackHandler(webView = { currentWebview })

        WebView(
            url = getUrl(),
            domStorageEnabled = true,
            webViewClient = CustomWebViewClient(
                onPageSucessfullyLoaded = { webView ->
                    // Waiting to get the WebView to inject CSS
                    applySafeAreaInsetsToWebView(webView, insets, density, layoutDirection)
                    mainViewModel.hasSeenWebView = true
                    keepSplashScreen.update { false }
                },
                onPageFailedToLoad = {
                    mainViewModel.logout()
                }
            ),
            webChromeClient = getCustomWebChromeClient(),
            withSafeArea = false,
            getWebView = { webview ->
                webview.addJavascriptInterface(jsBridge, JavascriptBridge.NAME)
                currentWebview = webview
            },
        )
    }

    private fun getUrl(): String {
        val urlFromWidget = intent.getStringExtra(EXTRA_URL)
        return urlFromWidget ?: (getProcessedDeeplinkUrl() ?: EURIA_MAIN_URL)
    }

    private fun applySafeAreaInsetsToWebView(
        webView: WebView?,
        insets: WindowInsets,
        density: Density,
        layoutDirection: LayoutDirection,
    ) {
        val top = insets.getTop(density).toDp(this)
        val right = insets.getRight(density, layoutDirection).toDp(this)
        val bottom = insets.getBottom(density).toDp(this)
        val left = insets.getLeft(density, layoutDirection).toDp(this)

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

        webView?.evaluateJavascript(script, null)
    }

    @Composable
    private fun HandleBackHandler(webView: () -> WebView?) {
        BackHandler {
            if (mainViewModel.hasSeenWebView) {
                webView()?.evaluateJavascript("window.goBack()", null)
            } else {
                finish()
            }
        }
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

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun AskMicrophonePermission() {
        val microphonePermissionState = rememberMultiplePermissionsState(
            permissions = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS)
        )

        LaunchedEffect(mainViewModel.microphonePermissionRequest) {
            mainViewModel.microphonePermissionRequest?.let {
                microphonePermissionState.launchMultiplePermissionRequest()
            }
        }

        LaunchedEffect(microphonePermissionState.allPermissionsGranted) {
            mainViewModel.microphonePermissionRequest?.let { microphonePermissionRequest ->
                if (microphonePermissionState.allPermissionsGranted) {
                    microphonePermissionRequest.grant(microphonePermissionRequest.resources)
                } else {
                    microphonePermissionRequest.deny()
                }
            }
        }
    }

    private fun getCustomWebChromeClient(): CustomWebChromeClient {
        return CustomWebChromeClient(
            onShowFileChooser = { filePathCallback, _ ->
                this@MainActivity.filePathCallback = filePathCallback
                mainViewModel.launchMediaChooser = true
                true
            },
            onRequestMicrophonePermission = { permissionRequest ->
                val hasRecordAudioPermission = hasPermission(Manifest.permission.RECORD_AUDIO)
                val hasModifyAudioPermission = hasPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)

                if (hasRecordAudioPermission && hasModifyAudioPermission) {
                    permissionRequest.grant(permissionRequest.resources)
                    mainViewModel.microphonePermissionRequest = null
                } else {
                    mainViewModel.microphonePermissionRequest = permissionRequest
                }
            },
        )
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun runLogin(): Nothing = coroutineScope {
        launch {
            mainViewModel.handleLogin(
                loginRequest,
                openLoginWebView = { openLoginWebView() },
                attemptLogin = {
                    val result = crossAppLoginViewModel.attemptLogin(it)
                    // IMPORTANT NOTE: The code below assumes singleSelection.
                    val token = result.tokens.singleOrNull()
                    if (token != null) {
                        mainViewModel.saveUserInfo(token) { error ->
                            showError(error)
                        }
                    } else {
                        val errorMessageId = result.errorMessageIds.singleOrNull()
                            ?: RCore.string.anErrorHasOccurred
                        showError(getString(errorMessageId))
                    }
                },
            )
        }
        crossAppLoginViewModel.activateUpdates(this@MainActivity, singleSelection = true)
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

    private fun Int.toDp(density: Density): Dp = with(density) { this@toDp.toDp() }

    companion object {
        const val TAG = "MainActivity"

        const val EXTRA_URL = "EXTRA_URL"

        // This tag is named like that just to have a unique identifier but the Web page does not rely on it
        private const val CSS_TAG_ID = "mobile-inset-style"

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
