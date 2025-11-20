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

package com.infomaniak.euria.ui.noNetwork

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.infomaniak.core.ui.compose.basics.Typography
import com.infomaniak.core.ui.compose.margin.Margin
import com.infomaniak.euria.R
import com.infomaniak.euria.ui.theme.Dimens
import com.infomaniak.euria.ui.theme.EuriaTheme

@Composable
fun NoNetworkScreen(modifier: Modifier = Modifier) {
    Box {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.radial_gradient_top_left),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = Dimens.OnboardingGradientPadding),
            contentScale = ContentScale.FillBounds,
        )

        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = Dimens.DescriptionWidth)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(ImageVector.vectorResource(R.drawable.circle_cloud_slash), contentDescription = null)
                    Spacer(modifier = Modifier.height(Margin.Huge))

                    Text(
                        text = stringResource(R.string.noNetworkTitle),
                        textAlign = TextAlign.Center,
                        style = Typography.h1,
                        color = EuriaTheme.colors.primaryTextColor,
                    )
                    Spacer(modifier = Modifier.height(Margin.Medium))

                    Text(
                        text = stringResource(R.string.noNetworkDescription),
                        textAlign = TextAlign.Center,
                        style = Typography.bodyRegular,
                        color = EuriaTheme.colors.secondaryTextColor,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    EuriaTheme {
        Surface {
            NoNetworkScreen()
        }
    }
}
