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

package com.infomaniak.euria.ui.login.components

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.infomaniak.core.crossapplogin.back.BaseCrossAppLoginViewModel.Companion.filterSelectedAccounts
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.crossapplogin.front.components.CrossLoginBottomContent
import com.infomaniak.core.onboarding.OnboardingPage
import com.infomaniak.core.onboarding.OnboardingScaffold
import com.infomaniak.core.onboarding.components.OnboardingComponents
import com.infomaniak.core.onboarding.components.OnboardingComponents.DefaultBackground
import com.infomaniak.euria.R
import com.infomaniak.euria.ui.theme.Dimens
import com.infomaniak.euria.ui.theme.EuriaTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    accounts: () -> List<ExternalAccount>,
    skippedIds: () -> Set<Long>,
    isLoginButtonLoading: () -> Boolean,
    isSignUpButtonLoading: () -> Boolean,
    onLoginRequest: (accounts: List<ExternalAccount>) -> Unit,
    onCreateAccount: () -> Unit,
    onSaveSkippedAccounts: (Set<Long>) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { Page.entries.size })

    val isHighlighted = Page.entries.associateWith { rememberSaveable { mutableStateOf(false) } }

    // Start the highlighting of the text when the associated page is reached in the HorizontalPager
    LaunchedEffect(pagerState.currentPage) {
        val currentPage = Page.entries[pagerState.currentPage]
        isHighlighted[currentPage]?.value = true
    }

    OnboardingScaffold(
        pagerState = pagerState,
        onboardingPages = Page.entries.mapIndexed { index, page ->
            page.toOnboardingPage(
                isHighlighted, pagerState, index
            )
        },
        bottomContent = { paddingValues ->
            OnboardingComponents.CrossLoginBottomContent(
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues),
                pagerState = pagerState,
                accounts = accounts,
                skippedIds = skippedIds,
                isLoginButtonLoading = isLoginButtonLoading,
                isSignUpButtonLoading = isSignUpButtonLoading,
                titleColor = EuriaTheme.colors.primaryTextColor,
                descriptionColor = EuriaTheme.colors.secondaryTextColor,
                onLogin = { onLoginRequest(emptyList()) },
                onContinueWithSelectedAccounts = {
                    onLoginRequest(
                        accounts().filterSelectedAccounts(
                            skippedIds()
                        )
                    )
                },
                onCreateAccount = onCreateAccount,
                onUseAnotherAccountClicked = { onLoginRequest(emptyList()) },
                onSaveSkippedAccounts = onSaveSkippedAccounts,
            )
        },
    )
}

val onboardingTitle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Light,
    fontSize = 22.sp,
    lineHeight = 28.sp,
)

val onboardingDescription = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
)

@Composable
private fun Page.toOnboardingPage(
    isHighlighted: Map<Page, MutableState<Boolean>>,
    pagerState: PagerState,
    index: Int,
): OnboardingPage =
    OnboardingPage(background = {
        DefaultBackground(
            ImageVector.vectorResource(backgroundRes),
            modifier = Modifier.padding(bottom = 300.dp)
        )
    }, illustration = {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(
                illustrationRes
            )
        )

        LottieAnimation(
            composition,
            restartOnPlay = isAnimationLooping,
            reverseOnRepeat = isAnimationLooping,
            iterations = if (isAnimationLooping) LottieConstants.IterateForever else 1,
            isPlaying = pagerState.currentPage == index,
            modifier = Modifier.width(illustrationSize),
        )

    }, text = {
        EuriaHighlightedTitleAndDescription(
            isHighlighted = { isHighlighted[this]?.value ?: false },
            title = stringResource(this.titleRes),
            subtitleTemplate = this.descriptionTemplateRes?.let { stringResource(it) } ?: "%s",
            subtitleArgument = stringResource(this.descriptionArgumentRes),
            highlightedAngleDegree = this.highlightedAngleDegree
        )
    })

private val DEFAULT_ILLUSTRATION_SIZE = 250.dp

private enum class Page(
    @DrawableRes val backgroundRes: Int,
    @RawRes val illustrationRes: Int,
    val isAnimationLooping: Boolean = true,
    val illustrationSize: Dp = DEFAULT_ILLUSTRATION_SIZE,
    @StringRes val titleRes: Int,
    @StringRes val descriptionTemplateRes: Int? = null,
    @StringRes val descriptionArgumentRes: Int,
    val highlightedAngleDegree: Double = Dimens.HighlightedAngleDegree,
) {
    Euria(
        illustrationRes = R.raw.euria_blob,
        backgroundRes = R.drawable.radial_gradient_top_right,
        titleRes = R.string.onboardingEuriaTitle,
        descriptionTemplateRes = null,
        descriptionArgumentRes = R.string.onboardingEuriaDescription,
    ),
    Privacy(
        illustrationRes = R.raw.euria_bubble,
        isAnimationLooping = false,
        illustrationSize = 400.dp,
        backgroundRes = R.drawable.radial_gradient_top_left,
        titleRes = R.string.onboardingPrivacyTitle,
        descriptionTemplateRes = R.string.onboardingPrivacyDescriptionTemplate,
        descriptionArgumentRes = R.string.onboardingPrivacyDescriptionArgument,
    ),
    Ephemeral(
        illustrationRes = R.raw.euria_ghost,
        backgroundRes = R.drawable.radial_gradient_top_right,
        titleRes = R.string.onboardingEphemeralTitle,
        descriptionTemplateRes = R.string.onboardingEphemeralDescriptionTemplate,
        descriptionArgumentRes = R.string.onboardingEphemeralDescriptionArguments,
    ),
    ReadyToStart(
        illustrationRes = R.raw.euria_blob,
        backgroundRes = R.drawable.radial_gradient_top_left,
        titleRes = R.string.onboardingLoginTitle,
        descriptionTemplateRes = R.string.onboardingLoginTemplate,
        descriptionArgumentRes = R.string.onboardingLoginArguments,
    ),
}
