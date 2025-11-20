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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.infomaniak.core.ui.compose.basicbutton.BasicButton
import com.infomaniak.core.ui.compose.basics.Typography
import com.infomaniak.core.ui.compose.margin.Margin
import com.infomaniak.euria.ui.theme.Dimens

@Composable
fun LargeButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: () -> Boolean = { true },
    showIndeterminateProgress: () -> Boolean = { false },
    progress: (() -> Float)? = null,
    onClick: () -> Unit,
    imageVector: ImageVector? = null,
) {
    BasicButton(
        onClick = onClick,
        modifier = modifier.height(Dimens.PrimaryButtonHeight),
        shape = Dimens.PrimaryButtonShape,
        enabled = enabled,
        showIndeterminateProgress = showIndeterminateProgress,
        progress = progress,
    ) {
        ButtonTextContent(imageVector, title)
    }
}

@Composable
private fun ButtonTextContent(imageVector: ImageVector?, title: String) {
    imageVector?.let {
        Icon(modifier = Modifier.size(Dimens.SmallIconSize), imageVector = it, contentDescription = null)
        Spacer(Modifier.width(Margin.Mini))
    }
    Text(text = title, style = Typography.bodyMedium)
}
