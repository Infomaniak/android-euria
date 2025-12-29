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
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.extensions.parcelable
import com.infomaniak.core.extensions.parcelableArrayList
import com.infomaniak.core.extensions.serializableExtra
import com.infomaniak.core.inappreview.reviewmanagers.InAppReviewManager
import com.infomaniak.core.observe
import com.infomaniak.core.twofactorauth.back.TwoFactorAuthManager
import com.infomaniak.core.twofactorauth.front.TwoFactorAuthApprovalAutoManagedBottomSheet
import com.infomaniak.core.ui.compose.basics.CallableState
import com.infomaniak.euria.MainViewModel.UserState
import com.infomaniak.euria.ui.EuriaMainScreen
import com.infomaniak.euria.ui.login.CrossAppLoginViewModel
import com.infomaniak.euria.ui.login.components.OnboardingScreen
import com.infomaniak.euria.ui.noNetwork.NoNetworkScreen
import com.infomaniak.euria.ui.theme.EuriaTheme
import com.infomaniak.euria.ui.widget.EuriaAppWidgetProvider.Companion.EXTRA_ACTION_CAMERA
import com.infomaniak.euria.upload.UploadManager
import com.infomaniak.euria.utils.AccountUtils
import com.infomaniak.euria.utils.WebViewUtils
import com.infomaniak.euria.webview.JavascriptBridge
import com.infomaniak.lib.login.InfomaniakLogin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import javax.inject.Inject
import com.infomaniak.core.R as RCore

val twoFactorAuthManager = TwoFactorAuthManager { userId -> AccountUtils.getHttpClient(userId) }

@AndroidEntryPoint
@OptIn(ExperimentalSplittiesApi::class)
class MainActivity : ComponentActivity(), AppReviewManageable {

    private val mainViewModel: MainViewModel by viewModels()
    private val crossAppLoginViewModel: CrossAppLoginViewModel by viewModels()
    private val takePicturePreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) lifecycleScope.launch { uploadManager.uploadBitmap(webViewUtils.webView, bitmap) }
        }

    @Inject
    lateinit var uploadManager: UploadManager

    override val inAppReviewManager by lazy { InAppReviewManager(this) }

    private val webViewUtils: WebViewUtils by lazy {
        WebViewUtils(
            context = applicationContext,
            javascriptBridge = JavascriptBridge(
                onLogin = { openLoginWebView() },
                onLogout = { mainViewModel.logout() },
                onUnauthenticated = { mainViewModel.logout() },
                onSignUp = { startAccountCreation() },
                onKeepDeviceAwake = { shouldKeepScreenOn ->
                    runOnUiThread {
                        if (shouldKeepScreenOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                },
                onReady = { mainViewModel.isWebAppReady(true) },
                onDismissApp = { finish() },
                onCancelFileUpload = { localId -> uploadManager.cancelUpload(localId) },
                onOpenCamera = { takePicturePreviewLauncher.launch(null) },
                onOpenReview = { mainViewModel.shouldShowInAppReview(true) },
                onUpgrade = { startAccountUpgrade() },
            )
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
                        authCode?.isNotBlank() == true -> {
                            mainViewModel.authenticateUser(
                                authCode,
                                forceRefreshWebView = { webViewUtils.webView?.reload() },
                                showError = { showError(it) }
                            )
                        }
                        else -> showError(getString(RCore.string.anErrorHasOccurred))
                    }
                }
            }
        }
    private val createAccountResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            result.handleCreateAccountActivityResult()
        }

    private var keepSplashScreen = MutableStateFlow(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel.initCurrentUser()
        initAppReviewManager()
        val splashScreen = installSplashScreen().apply { setKeepOnScreenCondition { true } }

        keepSplashScreen.observe(lifecycleOwner = this, state = Lifecycle.State.CREATED) {
            splashScreen.setKeepOnScreenCondition { it }
        }

        if (!enableWebViewDebugging()) return

        enableEdgeToEdge()
        if (SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        lifecycleScope.launch {
            mainViewModel.userState.collectLatest {
                if (it is UserState.NotLoggedIn) runLogin()
            }
        }

        executeIntentAction(intent)

        setContent {
            EuriaTheme {
                val accountsCheckingState by crossAppLoginViewModel.accountsCheckingState.collectAsStateWithLifecycle()
                val skippedIds by crossAppLoginViewModel.skippedAccountIds.collectAsStateWithLifecycle()
                val userState by mainViewModel.userState.collectAsStateWithLifecycle()
                val isNetworkAvailable by mainViewModel.isNetworkAvailable.collectAsStateWithLifecycle()

                Surface {
                    when {
                        userState is UserState.Loading -> Unit
                        userState is UserState.NotLoggedIn && !mainViewModel.skipOnboarding -> {
                            keepSplashScreen.update { false }
                            OnboardingScreen(
                                accountsCheckingState = { accountsCheckingState },
                                skippedIds = { skippedIds },
                                isLoginButtonLoading = { loginRequest.isAwaitingCall.not() },
                                onLoginRequest = { accounts -> loginRequest(accounts) },
                                onSaveSkippedAccounts = { crossAppLoginViewModel.skippedAccountIds.value = it },
                                onStartClicked = { mainViewModel.skipOnboarding(true) },
                            )
                        }
                        !isNetworkAvailable && !mainViewModel.hasSeenWebView -> {
                            keepSplashScreen.update { false }
                            NoNetworkScreen()
                        }
                        else -> {
                            // We can arrive here with a UserState.NotLoggedIn state because of Euria free
                            val userState = userState as? UserState.LoggedIn
                            EuriaMainScreen(
                                mainViewModel = mainViewModel,
                                inAppReviewManager = inAppReviewManager,
                                uploadManager = uploadManager,
                                webViewUtils = webViewUtils,
                                token = userState?.user?.apiToken?.accessToken,
                                keepSplashScreen = { state -> keepSplashScreen.update { state } },
                                finishApp = { finish() },
                                startCamera = { takePicturePreviewLauncher.launch(null) }
                            )
                        }
                    }

                    TwoFactorAuthApprovalAutoManagedBottomSheet(twoFactorAuthManager)
                }
            }
        }
    }

    private fun enableWebViewDebugging(): Boolean {
        return runCatching {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        }.fold(
            onSuccess = { true },
            onFailure = { throwable ->
                if (throwable.hasCause("MissingWebViewPackageException")) {
                    showWebViewNotAvailableError()
                    false
                } else {
                    throw throwable
                }
            }
        )
    }

    private fun Throwable.hasCause(className: String): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current.javaClass.name.contains(className)) return true
            current = current.cause
        }
        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        executeIntentAction(intent)
    }

    private fun showWebViewNotAvailableError() {
        MaterialAlertDialogBuilder(this, R.style.EuriaDialog)
            .setTitle(getString(RCore.string.anErrorHasOccurred))
            .setMessage(getString(R.string.noWebViewAvailable))
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun executeIntentAction(intent: Intent) {
        intent.serializableExtra<MatomoEuria.MatomoName>(EXTRA_MATOMO_WIDGET_NAME)?.let {
            MatomoEuria.trackWidgetEvent(it)
        }

        if (intent.getStringExtra(EXTRA_ACTION) == EXTRA_ACTION_CAMERA) mainViewModel.cameraLaunchEvents.trySend(Unit)

        webViewUtils.updateWebViewQueryFrom(intent, updateWebViewQuery = { query ->
            mainViewModel.webViewQueries.trySend(query)
        })

        extractFilesToShare(intent)
    }

    private fun extractFilesToShare(intent: Intent) {
        val uris = buildList {
            if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)?.let {
                    addAll(it.toList())
                }
            } else if (intent.action == Intent.ACTION_SEND) {
                var clipDataContainsUris = false
                intent.clipData?.getItemAt(0)?.uri?.let {
                    add(it)
                    hasUrisBeenFetched = true
                }
                if (!hasUrisBeenFetched) {
                    intent.parcelable<Uri>(Intent.EXTRA_STREAM)?.let {
                        add(it)
                    }
                }
            }
        }

        if (uris.isNotEmpty()) {
            mainViewModel.setFilesToShare(uris)
            intent.action = ""
            intent.clipData = null
            intent.removeExtra(Intent.EXTRA_STREAM)
        }
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
        mainViewModel.infomaniakLogin.startWebViewLogin(webViewLoginResultLauncher)
    }

    private fun showError(error: String) {
        Snackbar.make(
            window.decorView.findViewById(android.R.id.content),
            error,
            Snackbar.LENGTH_LONG,
        ).show()
    }

    private fun startAccountCreation() {
        mainViewModel.infomaniakLogin.startCreateAccountWebView(
            resultLauncher = createAccountResultLauncher,
            createAccountUrl = CREATE_ACCOUNT_URL,
            successHost = CREATE_ACCOUNT_SUCCESS_HOST,
            cancelHost = CREATE_ACCOUNT_CANCEL_HOST,
        )
    }

    private fun startAccountUpgrade() {
        mainViewModel.infomaniakLogin.startCreateAccountWebView(
            resultLauncher = createAccountResultLauncher,
            createAccountUrl = UPGRADE_ACCOUNT_URL,
            successHost = CREATE_ACCOUNT_SUCCESS_HOST,
            cancelHost = CREATE_ACCOUNT_CANCEL_HOST,
            headers = mapOf("Authorization" to "Bearer ${AccountUtils.currentUser?.apiToken?.accessToken}"),
            removeCookies = false,
            ignoreFirstCancelUrl = true,
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
        }
    }

    object PendingIntentRequestCodes {
        const val CHAT = 0
        const val EPHEMERAL = 1
        const val SPEECH = 2
        const val CAMERA = 3
    }

    companion object {
        const val TAG = "MainActivity"

        const val EXTRA_MATOMO_WIDGET_NAME = "EXTRA_MATOMO_NAME"
        const val EXTRA_ACTION = "EXTRA_ACTION"
        const val EXTRA_QUERY = "EXTRA_QUERY"

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
