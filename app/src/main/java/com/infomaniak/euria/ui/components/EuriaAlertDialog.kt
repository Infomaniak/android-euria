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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.infomaniak.core.ui.compose.margin.Margin
import com.infomaniak.euria.ui.theme.EuriaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EuriaAlertDialog(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    positiveButton: @Composable () -> Unit,
    negativeButton: @Composable () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
    ) {
        Card(shape = RoundedCornerShape(Margin.Medium)) {
            BasicAlertDialogContent(
                modifier = modifier,
                title = title,
                positiveButton = positiveButton,
                negativeButton = negativeButton,
                description = description,
                additionalContent = content,
            )
        }
    }
}

@Composable
private fun BasicAlertDialogContent(
    modifier: Modifier,
    title: String,
    description: String?,
    positiveButton: @Composable () -> Unit,
    negativeButton: @Composable () -> Unit,
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null,
) {
    Column(modifier.padding(Margin.Large)) {
        TitleAndDescription(title, description)
        Spacer(Modifier.height(Margin.Large))
        additionalContent?.let {
            it()
            Spacer(Modifier.height(Margin.Mini))
        }
        ActionButtons(positiveButton, negativeButton)
    }
}

@Composable
private fun TitleAndDescription(title: String, description: String?) {
    Text(
        text = title,
        style = EuriaTheme.typography.bodyMedium,
        color = EuriaTheme.colors.primaryTextColor,
    )
    description?.let {
        Spacer(Modifier.height(Margin.Large))
        Text(
            text = description,
            style = EuriaTheme.typography.bodyRegular,
            color = EuriaTheme.colors.secondaryTextColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionButtons(
    positiveButton: @Composable () -> Unit,
    negativeButton: @Composable () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        negativeButton()
        positiveButton()
    }
}
