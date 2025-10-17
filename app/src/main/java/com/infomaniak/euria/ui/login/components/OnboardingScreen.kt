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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.infomaniak.core.compose.basics.Typography
import com.infomaniak.core.crossapplogin.back.BaseCrossAppLoginViewModel.Companion.filterSelectedAccounts
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.crossapplogin.front.components.CrossLoginBottomContent
import com.infomaniak.core.onboarding.OnboardingPage
import com.infomaniak.core.onboarding.OnboardingScaffold
import com.infomaniak.core.onboarding.components.OnboardingComponents
import com.infomaniak.core.onboarding.components.OnboardingComponents.DefaultBackground
import com.infomaniak.core.onboarding.components.OnboardingComponents.DefaultTitleAndDescription
import com.infomaniak.euria.R
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

    OnboardingScaffold(
        pagerState = pagerState,
        onboardingPages = Page.entries.mapIndexed { index, page ->
            page.toOnboardingPage(
                pagerState, index
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

@Composable
private fun Page.toOnboardingPage(pagerState: PagerState, index: Int): OnboardingPage =
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
            restartOnPlay = true,
            reverseOnRepeat = true,
            iterations = LottieConstants.IterateForever,
            isPlaying = pagerState.currentPage == index,
            modifier = Modifier.height(150.dp)
        )
    }, text = {
        DefaultTitleAndDescription(
            title = stringResource(titleRes),
            description = stringResource(descriptionRes),
            titleStyle = Typography.h2.copy(color = EuriaTheme.colors.primaryTextColor),
            descriptionStyle = Typography.bodyRegular.copy(color = EuriaTheme.colors.secondaryTextColor),
        )
    })

private enum class Page(
    @DrawableRes val backgroundRes: Int,
    @RawRes val illustrationRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
) {
    WhoIsEuria(
        backgroundRes = R.drawable.vertical_gradient,
        illustrationRes = R.raw.euria,
        titleRes = R.string.onboardingFirstPageTitle,
        descriptionRes = R.string.onboardingFirstPageDescription,
    ),
    OurValues(
        backgroundRes = R.drawable.vertical_gradient,
        illustrationRes = R.raw.euria,
        titleRes = R.string.onboardingSecondPageTitle,
        descriptionRes = R.string.onboardingSecondPageDescription,
    ),
    BuiltIntoInfomaniakTools(
        backgroundRes = R.drawable.vertical_gradient,
        illustrationRes = R.raw.euria,
        titleRes = R.string.onboardingThirdPageTitle,
        descriptionRes = R.string.onboardingThirdPageDescription,
    ),
}
