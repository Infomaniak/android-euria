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

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.infomaniak.euria.R
import com.infomaniak.core.R as RCore

@Composable
fun ReviewAlertDialog(onUserWantsToReview: () -> Unit, onUserWantsToGiveFeedback: () -> Unit, onDismiss: () -> Unit) {
    EuriaAlertDialog(
        title = stringResource(RCore.string.reviewAlertTitle, stringResource(R.string.appName)),
        positiveButton = {
            SmallButton(
                style = ButtonType.Tertiary,
                title = stringResource(RCore.string.buttonYes),
                onClick = onUserWantsToReview,
            )
        },
        negativeButton = {
            SmallButton(
                style = ButtonType.Tertiary,
                title = stringResource(RCore.string.buttonNo),
                onClick = onUserWantsToGiveFeedback,
            )
        },
        onDismiss = onDismiss,
    )
}
