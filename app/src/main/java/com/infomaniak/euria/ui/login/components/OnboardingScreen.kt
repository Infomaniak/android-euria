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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.infomaniak.core.crossapplogin.back.BaseCrossAppLoginViewModel.AccountsCheckingState
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.crossapplogin.front.components.CrossLoginBottomContent
import com.infomaniak.core.crossapplogin.front.components.NoCrossAppLoginAccountsContent
import com.infomaniak.core.crossapplogin.front.data.CrossLoginDefaults
import com.infomaniak.core.onboarding.OnboardingPage
import com.infomaniak.core.onboarding.OnboardingScaffold
import com.infomaniak.core.onboarding.components.OnboardingComponents
import com.infomaniak.core.onboarding.components.OnboardingComponents.DefaultBackground
import com.infomaniak.core.ui.compose.margin.Margin
import com.infomaniak.euria.R
import com.infomaniak.euria.ui.theme.Dimens
import com.infomaniak.euria.ui.theme.EuriaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    accountsCheckingState: () -> AccountsCheckingState,
    skippedIds: () -> Set<Long>,
    isLoginButtonLoading: () -> Boolean,
    onLoginRequest: (accounts: List<ExternalAccount>) -> Unit,
    onSaveSkippedAccounts: (Set<Long>) -> Unit,
    onStartClicked: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { Page.entries.size })

    val isHighlighted = Page.entries.associateWith { rememberSaveable { mutableStateOf(false) } }

    // Start the highlighting of the text when the associated page is reached in the HorizontalPager
    LaunchedEffect(pagerState.currentPage) {
        val currentPage = Page.entries[pagerState.currentPage]
        isHighlighted[currentPage]?.value = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        OnboardingScaffold(
            pagerState = pagerState,
            onboardingPages = Page.entries.mapIndexed { _, page ->
                page.toOnboardingPage(isHighlighted)
            },
            bottomContent = { paddingValues ->
                OnboardingComponents.CrossLoginBottomContent(
                    modifier = Modifier
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues),
                    pagerState = pagerState,
                    accountsCheckingState = accountsCheckingState,
                    skippedIds = skippedIds,
                    isSingleSelection = true,
                    isLoginButtonLoading = isLoginButtonLoading,
                    customization = CrossLoginDefaults.customize(
                        colors = CrossLoginDefaults.colors(
                            titleColor = EuriaTheme.colors.primaryTextColor,
                            descriptionColor = EuriaTheme.colors.secondaryTextColor
                        ),
                    ),
                    onContinueWithSelectedAccounts = { selectedAccounts -> onLoginRequest(selectedAccounts) },
                    onUseAnotherAccountClicked = { onLoginRequest(emptyList()) },
                    onSaveSkippedAccounts = onSaveSkippedAccounts,
                    noCrossAppLoginAccountsContent = NoCrossAppLoginAccountsContent.accountOptional { onStartClicked() }
                )
            },
        )

        Image(
            modifier = Modifier
                .width(350.dp)
                .wrapContentHeight()
                .safeDrawingPadding()
                .padding(top = Margin.Large)
                .align(Alignment.TopCenter),
            painter = painterResource(id = R.drawable.infomaniak_logo),
            contentDescription = null,
        )
    }
}

@Composable
private fun Page.toOnboardingPage(
    isHighlighted: Map<Page, MutableState<Boolean>>
): OnboardingPage =
    OnboardingPage(background = {
        DefaultBackground(
            ImageVector.vectorResource(backgroundRes),
            modifier = Modifier.padding(bottom = Dimens.OnboardingGradientPadding)
        )
    }, illustration = {
        OnboardingPageIllustration()
    }, text = {
        EuriaHighlightedTitleAndDescription(
            isHighlighted = { isHighlighted[this]?.value ?: false },
            title = stringResource(titleRes),
            subtitleTemplate = descriptionTemplateRes?.let { stringResource(it) } ?: "%s",
            subtitleArgument = stringResource(descriptionArgumentRes),
            highlightedAngleDegree = highlightedAngleDegree
        )
    })

@Composable
private fun Page.OnboardingPageIllustration() {
    when (illustration) {
        is IllustrationResource.Static -> {
            Image(
                modifier = Modifier.size(350.dp),
                painter = painterResource(id = illustration.res),
                contentDescription = null,
            )
        }
        is IllustrationResource.Animated -> {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(illustration.res))

            LottieAnimation(
                composition,
                isPlaying = true,
                reverseOnRepeat = true,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.height(270.dp),
            )
        }
    }
}

sealed class IllustrationResource {
    data class Static(@DrawableRes val res: Int) : IllustrationResource()
    data class Animated(@RawRes val res: Int) : IllustrationResource()
}

private enum class Page(
    val illustration: IllustrationResource,
    @DrawableRes val backgroundRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionTemplateRes: Int? = null,
    @StringRes val descriptionArgumentRes: Int,
    val highlightedAngleDegree: Double = Dimens.HighlightedAngleDegree,
) {
    Euria(
        illustration = IllustrationResource.Animated(R.raw.euria),
        backgroundRes = R.drawable.radial_gradient_center_right,
        titleRes = R.string.onboardingEuriaTitle,
        descriptionTemplateRes = null,
        descriptionArgumentRes = R.string.onboardingEuriaDescription,
    ),
    DataCenter(
        illustration = IllustrationResource.Static(R.drawable.data_center),
        backgroundRes = R.drawable.radial_gradient_center_left,
        titleRes = R.string.onboardingDatacenterTitle,
        descriptionTemplateRes = null,
        descriptionArgumentRes = R.string.onboardingDatacenterDescription,
    ),
    Ephemeral(
        illustration = IllustrationResource.Animated(R.raw.euria_ghost),
        backgroundRes = R.drawable.radial_gradient_center_right,
        titleRes = R.string.onboardingEphemeralTitle,
        descriptionTemplateRes = R.string.onboardingEphemeralDescriptionTemplate,
        descriptionArgumentRes = R.string.onboardingEphemeralDescriptionArguments,
    ),
    Privacy(
        illustration = IllustrationResource.Static(R.drawable.mountain),
        backgroundRes = R.drawable.radial_gradient_center_left,
        titleRes = R.string.onboardingPrivacyTitle,
        descriptionTemplateRes = null,
        descriptionArgumentRes = R.string.onboardingPrivacyDescription,
    ),
    ReadyToStart(
        illustration = IllustrationResource.Animated(R.raw.euria),
        backgroundRes = R.drawable.radial_gradient_center_right,
        titleRes = R.string.onboardingLoginTitle,
        descriptionTemplateRes = R.string.onboardingLoginTemplate,
        descriptionArgumentRes = R.string.onboardingLoginArguments,
    ),
}
