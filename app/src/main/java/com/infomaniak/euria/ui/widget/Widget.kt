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

@file:SuppressLint("RestrictedApi")

package com.infomaniak.euria.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.infomaniak.core.compose.margin.Margin
import com.infomaniak.euria.EURIA_MAIN_URL
import com.infomaniak.euria.EURIA_WIDGET_EPHEMERAL
import com.infomaniak.euria.EURIA_WIDGET_MICROPHONE
import com.infomaniak.euria.MainActivity
import com.infomaniak.euria.MainActivity.Companion.EXTRA_URL
import com.infomaniak.euria.R
import com.infomaniak.euria.ui.theme.Dimens

class EuriaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Widget()
}

class Widget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // We have to pass the context because stringResource is not working in a Glance context (Widget)
            EuriaWidget(context)
        }
    }
}

@Composable
fun EuriaWidget(context: Context) {
    Column(
        modifier = GlanceModifier
            .wrapContentSize()
            .padding(Margin.Small)
            .background(imageProvider = getImage(R.drawable.widget_background)),
    ) {
        NewConversationButton(widgetFieldText = context.getString(R.string.widgetFieldText))
        Spacer(modifier = GlanceModifier.height(Dimens.SpaceBetweenWidget))
        ActionButtons()
    }
}

@Composable
private fun ActionButtons() {
    Row(
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buttons = listOf(WidgetAction.Ephemeral, WidgetAction.Microphone)
        buttons.forEach { widgetButton ->
            IconWithBackground(
                drawableRes = widgetButton.iconRes,
                onClick = widgetButton.action,
                withSpacer = widgetButton != buttons.last(),
            )
        }
    }
}

@Composable
private fun NewConversationButton(widgetFieldText: String) {
    Row(
        modifier = GlanceModifier
            .padding(Margin.Small)
            .fillMaxWidth()
            .background(imageProvider = getImage(R.drawable.round_corner_background))
            .clickable(getAction(EURIA_MAIN_URL)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = GlanceModifier
                .size(Dimens.WidgetIconSize),
            provider = getImage(R.drawable.euria),
            contentDescription = "",
        )
        Text(
            text = widgetFieldText,
            style = TextStyle(
                color = ColorProvider(R.color.widgetTextColor)
            ),
        )
    }
}

@Composable
private fun IconWithBackground(
    drawableRes: Int,
    onClick: Action,
    withSpacer: Boolean,
) {
    Box(
        modifier = GlanceModifier
            .background(imageProvider = getImage(R.drawable.round_corner_background))
            .padding(Margin.Small)
            .clickable(onClick)
            .wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            modifier = GlanceModifier
                .size(Dimens.WidgetIconSize)
                .padding(Margin.Small),
            provider = getImage(drawableRes),
            contentDescription = "",
            colorFilter = ColorFilter.tint(ColorProvider(R.color.widgetButtonImageTint))
        )
    }

    if (withSpacer) {
        Spacer(
            modifier = GlanceModifier
                .width(Dimens.SpaceBetweenWidget),
        )
    }
}

private fun getImage(drawableRes: Int) = AndroidResourceImageProvider(drawableRes)

private sealed class WidgetAction(val iconRes: Int, val action: Action) {
    object Ephemeral : WidgetAction(R.drawable.clock, getAction(EURIA_WIDGET_EPHEMERAL))
    object Microphone : WidgetAction(R.drawable.microphone, getAction(EURIA_WIDGET_MICROPHONE))
}

private fun getAction(url: String): Action {
    return actionStartActivity<MainActivity>(
        parameters = actionParametersOf(
            ActionParameters.Key<String>(EXTRA_URL) to url,
        )
    )
}
