package com.infomaniak.euria.ui.freeTrial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.infomaniak.core.compose.basics.Typography
import com.infomaniak.core.compose.bottomstickybuttonscaffolds.BottomStickyButtonScaffold
import com.infomaniak.core.onboarding.components.OnboardingComponents
import com.infomaniak.euria.R
import com.infomaniak.euria.ui.components.LargeButton
import com.infomaniak.euria.ui.theme.EuriaTheme

@Composable
fun FreeTrialScreen(modifier: Modifier = Modifier) {
    BottomStickyButtonScaffold(
        modifier = modifier,
        topBar = {},
        topButton = {
            LargeButton(
                modifier = it,
                title = "Démarrer gratuitement", // TODO
                onClick = { /*TODO*/ }
            )
        },
        bottomButton = {
            LargeButton(
                modifier = it,
                title = "Changer de compte", // TODO
                onClick = { /*TODO*/ }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround) {
                EuriaAnimation()
                OnboardingComponents.HighlightedTitleAndDescription(
                    title = "Vous n'avez pas encore",
                    subtitleTemplate = "%s",
                    subtitleArgument = "activé Euria",
                    textStyle = TODO(),
                    descriptionWidth = TODO(),
                    highlightedTextStyle = TODO(),
                    highlightedColor = TODO(),
                    highlightedAngleDegree = TODO(),
                    isHighlighted = { true }
                )
            }
            Text(
                text = "Activez Euria gratuitement dès maintenant", // TODO
                textAlign = TextAlign.Center,
                style = Typography.bodyRegular,
                color = EuriaTheme.colors.secondaryTextColor,
            )
        }
    }
}

@Composable
fun EuriaAnimation() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.euria))

    LottieAnimation(
        composition,
        restartOnPlay = true,
        reverseOnRepeat = true,
        iterations = LottieConstants.IterateForever,
        modifier = if (illustrationSize != null) Modifier.width(illustrationSize) else Modifier
    )
}

@Preview
@Composable
private fun Preview() {
    EuriaTheme {
        Surface {
            FreeTrialScreen()
        }
    }
}
