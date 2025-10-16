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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.infomaniak.core.compose.basics.Typography
import com.infomaniak.core.crossapplogin.back.BaseCrossAppLoginViewModel.Companion.filterSelectedAccounts
import com.infomaniak.core.crossapplogin.back.ExternalAccount
import com.infomaniak.core.crossapplogin.front.components.CrossLoginBottomContent
import com.infomaniak.core.onboarding.OnboardingPage
import com.infomaniak.core.onboarding.OnboardingScaffold
import com.infomaniak.core.onboarding.components.OnboardingComponents
import com.infomaniak.core.onboarding.components.OnboardingComponents.DefaultBackground
import com.infomaniak.core.onboarding.components.OnboardingComponents.DefaultLottieIllustration
import com.infomaniak.core.onboarding.components.OnboardingComponents.DefaultTitleAndDescription
import com.infomaniak.euria.R


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
                pagerState,
                index
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
                titleColor = colorResource(android.R.color.black),
                descriptionColor = colorResource(android.R.color.darker_gray),
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
    OnboardingPage(
        background = {
            DefaultBackground(
                ImageVector.vectorResource(backgroundRes),
                modifier = Modifier.padding(bottom = 300.dp)
            )
        },
        illustration = {
            DefaultLottieIllustration(
                lottieRawRes = illustrationRes,
                isCurrentPageVisible = { pagerState.currentPage == index },
                // Height of the biggest of the three illustrations. Because all animations don't have the same height, we need to
                // force them to have the same height so the content of every page is correctly aligned
                modifier = Modifier.height(270.dp)
            )
        },
        text = {
            DefaultTitleAndDescription(
                title = stringResource(titleRes),
                description = stringResource(descriptionRes),
                titleStyle = com.infomaniak.core.compose.basics.Typography.h2.copy(
                    color = colorResource(
                        android.R.color.black
                    )
                ),
                descriptionStyle = Typography.bodyRegular.copy(color = colorResource(android.R.color.darker_gray)),
            )
        }
    )

private enum class Page(
    @DrawableRes val backgroundRes: Int,
    @RawRes val illustrationRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
) {
    AccessFiles(
        backgroundRes = R.drawable.ic_launcher_background,
        illustrationRes = android.R.drawable.star_on,
        titleRes = R.string.app_name,
        descriptionRes = R.string.app_name,
    ),
}
