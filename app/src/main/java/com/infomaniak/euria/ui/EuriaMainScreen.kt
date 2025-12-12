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

package com.infomaniak.euria.ui

import android.Manifest
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.infomaniak.core.inappreview.reviewmanagers.InAppReviewManager
import com.infomaniak.core.network.networking.HttpUtils
import com.infomaniak.core.webview.ui.components.WebView
import com.infomaniak.euria.EURIA_MAIN_URL
import com.infomaniak.euria.MainViewModel
import com.infomaniak.euria.ui.components.ReviewAlertDialog
import com.infomaniak.euria.upload.UploadManager
import com.infomaniak.euria.utils.WebViewUtils
import com.infomaniak.euria.webview.CustomWebViewClient
import com.infomaniak.euria.webview.JavascriptBridge
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun EuriaMainScreen(
    mainViewModel: MainViewModel,
    inAppReviewManager: InAppReviewManager,
    uploadManager: UploadManager,
    webViewUtils: WebViewUtils,
    token: String?,
    keepSplashScreen: (Boolean) -> Unit,
    finishApp: () -> Unit,
) {
    webViewUtils.setTokenToCookie(token)

    var currentWebview: WebView? by remember { mutableStateOf(null) }
    var filePathCallback: ValueCallback<Array<out Uri>>? by remember { mutableStateOf(null) }
    val insets = WindowInsets.safeDrawing
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val shouldDisplayReviewDialog by mainViewModel.shouldShowInAppReview.collectAsStateWithLifecycle(initialValue = false)

    // Need this to apply the insets to the WebView when these ones changes
    currentWebview?.let { webViewUtils.applySafeAreaInsets(it, insets, density, layoutDirection) }

    AskMicrophonePermission(mainViewModel)
    ShowFileChooser(mainViewModel, filePathCallback)

    HandleBackHandler(
        hasSeenWebView = { mainViewModel.hasSeenWebView },
        webView = { currentWebview },
        finishApp = finishApp,
    )

    LaunchedEffect(Unit) {
        mainViewModel.isWebAppReady.collectLatest { isWebAppReady ->
            if (isWebAppReady) {
                launch {
                    mainViewModel.filesToShare.receiveAsFlow().collectLatest { filesUris ->
                        if (filesUris.isNotEmpty()) uploadManager.uploadFiles(currentWebview, filesUris)
                    }
                }
                launch {
                    mainViewModel.webViewQueries.receiveAsFlow().collect { query ->
                        currentWebview?.evaluateJavascript("goTo(\"$query\")", null)
                    }
                }
            }
        }
    }

    ReviewDialog(shouldDisplayReviewDialog, inAppReviewManager, mainViewModel)

    WebView(
        url = EURIA_MAIN_URL,
        userAgentString = HttpUtils.getUserAgent,
        domStorageEnabled = true,
        webViewClient = CustomWebViewClient(
            onPageSucessfullyLoaded = { webView ->
                // Waiting to get the WebView to inject CSS
                webViewUtils.applySafeAreaInsets(webView, insets, density, layoutDirection)
                mainViewModel.hasSeenWebView = true
                keepSplashScreen(false)
            },
        ),
        webChromeClient = webViewUtils.getCustomWebChromeClient(
            filePathCallback = { filePathCallback = it },
            launchMediaChooser = { mainViewModel.launchMediaChooser = it },
            microphonePermissionRequest = { mainViewModel.microphonePermissionRequest = it }
        ),
        withSafeArea = false,
        getWebView = { webView ->
            webView.addJavascriptInterface(webViewUtils.javascriptBridge, JavascriptBridge.NAME)
            currentWebview = webView
            webViewUtils.webView = webView
        },
    )
}

@Composable
private fun ReviewDialog(
    shouldDisplayReviewDialog: Boolean,
    inAppReviewManager: InAppReviewManager,
    mainViewModel: MainViewModel,
) {
    if (shouldDisplayReviewDialog) {
        with(inAppReviewManager) {
            ReviewAlertDialog(
                onUserWantsToReview = {
                    onUserWantsToReview()
                    mainViewModel.shouldShowInAppReview.value = false
                },
                onUserWantsToGiveFeedback = {
                    // Nothing to do here because we don't have a feedback URL yet
                    mainViewModel.shouldShowInAppReview.value = false
                },
                onDismiss = {
                    onUserWantsToDismiss()
                    mainViewModel.shouldShowInAppReview.value = false
                }
            )
        }
    }
}

@Composable
private fun HandleBackHandler(
    hasSeenWebView: () -> Boolean,
    webView: () -> WebView?,
    finishApp: () -> Unit,
) {
    BackHandler {
        if (hasSeenWebView()) {
            webView()?.evaluateJavascript("window.goBack()", null)
        } else {
            finishApp()
        }
    }
}

@Composable
private fun ShowFileChooser(
    mainViewModel: MainViewModel,
    filePathCallback: ValueCallback<Array<out Uri>>?,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            filePathCallback?.onReceiveValue(uris.toTypedArray())
            mainViewModel.launchMediaChooser = false
        }
    )

    if (mainViewModel.launchMediaChooser) launcher.launch(arrayOf("*/*"))
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun AskMicrophonePermission(mainViewModel: MainViewModel) {
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

            mainViewModel.microphonePermissionRequest = null
        }
    }
}
