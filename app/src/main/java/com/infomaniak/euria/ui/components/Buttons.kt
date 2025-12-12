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

package com.infomaniak.euria.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.infomaniak.core.ui.compose.basicbutton.BasicButton
import com.infomaniak.core.ui.compose.margin.Margin
import com.infomaniak.euria.ui.theme.CustomShapes
import com.infomaniak.euria.ui.theme.EuriaTheme

/**
 * Specifying a progress has the priority over specifying showIndeterminateProgress
 */
@Composable
fun SmallButton(
    title: String,
    modifier: Modifier = Modifier,
    style: ButtonType = ButtonType.Primary,
    enabled: () -> Boolean = { true },
    showIndeterminateProgress: () -> Boolean = { false },
    progress: (() -> Float)? = null,
    onClick: () -> Unit,
    imageVector: ImageVector? = null,
) {
    CoreButton(
        title,
        modifier,
        style,
        enabled,
        showIndeterminateProgress,
        progress,
        onClick,
        imageVector,
    )
}

@Composable
private fun CoreButton(
    title: String,
    modifier: Modifier,
    style: ButtonType,
    enabled: () -> Boolean,
    showIndeterminateProgress: () -> Boolean,
    progress: (() -> Float)?,
    onClick: () -> Unit,
    imageVector: ImageVector?,
) {
    BasicButton(
        onClick = onClick,
        modifier = modifier.height(ButtonSize.SMALL.height),
        shape = CustomShapes.MEDIUM,
        colors = style.colors(),
        enabled = enabled,
        showIndeterminateProgress = showIndeterminateProgress,
        progress = progress,
        contentPadding = ButtonSize.SMALL.contentPadding,
    ) {
        ButtonTextContent(imageVector, title)
    }
}

enum class ButtonType(val colors: @Composable () -> ButtonColors) {
    Primary({
        ButtonDefaults.buttonColors(
            containerColor = EuriaTheme.materialColors.primary,
            contentColor = EuriaTheme.materialColors.onPrimary,
        )
    }),

    Tertiary({
        ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = EuriaTheme.materialColors.primary,
            disabledContainerColor = Color.Transparent,
        )
    }),
}

private enum class ButtonSize(val height: Dp, val contentPadding: PaddingValues) {
    SMALL(40.dp, PaddingValues(horizontal = Margin.Medium)),
}
