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

package com.infomaniak.euria.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.infomaniak.euria.MainActivity
import com.infomaniak.euria.MainActivity.Companion.EXTRA_ACTION
import com.infomaniak.euria.MainActivity.Companion.EXTRA_QUERY
import com.infomaniak.euria.MainActivity.PendingIntentRequestCodes
import com.infomaniak.euria.R

class EuriaAppWidgetProvider : AppWidgetProvider() {

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetId, appWidgetManager)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val appWidgetId = appWidgetIds.firstOrNull() ?: return
        updateWidget(context, appWidgetId, appWidgetManager)
    }

    private fun updateWidget(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager,
    ) {
        // We only have on widget so we use the first one
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        val views = RemoteViews(context.packageName, R.layout.widget_view_flipper)

        val widgetType = WidgetType.getWidgetTypeFrom(minWidth, minHeight)

        views.setDisplayedChild(R.id.widget_view_flipper, widgetType.index)

        appWidgetManager.updateAppWidget(appWidgetId, views)

        val mainIntent = getIntent(context)
        val ephemeralIntent = getIntent(context, query = EPHEMERAL_QUERY)
        val microphoneIntent = getIntent(context, query = MICROPHONE_QUERY)
        val cameraIntent = getIntent(context, action = "CAMERA")

        views.setOnClickPendingIntent(
            R.id.newConversationButton,
            getPendingIntent(context, mainIntent, PendingIntentRequestCodes.CHAT)
        )
        views.setOnClickPendingIntent(
            R.id.ephemeralButton,
            getPendingIntent(context, ephemeralIntent, PendingIntentRequestCodes.EPHEMERAL)
        )
        views.setOnClickPendingIntent(
            R.id.microphoneButton,
            getPendingIntent(context, microphoneIntent, PendingIntentRequestCodes.SPEECH)
        )
        views.setOnClickPendingIntent(
            R.id.cameraButton,
            getPendingIntent(context, cameraIntent, PendingIntentRequestCodes.CAMERA)
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getIntent(context: Context, query: String? = null, action: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, action)
            putExtra(EXTRA_QUERY, query)
        }
    }

    private fun getPendingIntent(context: Context, intent: Intent, requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private enum class WidgetType(val index: Int, val minWidth: Int, val minHeight: Int) {
        TWO_ROWS(0, 250, 100),
        ONE_ROW_FOUR_ACTIONS(1, 300, 40),
        ONE_ROW_THREE_ACTIONS(2, 250, 40),
        ONE_ROW_TWO_ACTIONS(3, 150, 40),
        ONE_ROW_ONE_ACTION(4, 80, 40);

        companion object {
            fun getWidgetTypeFrom(minWidth: Int, minHeight: Int): WidgetType {
                return entries.firstOrNull { it.minWidth <= minWidth && it.minHeight <= minHeight } ?: ONE_ROW_ONE_ACTION
            }
        }
    }

    companion object {
        private const val EPHEMERAL_QUERY = "/?ephemeral=true"
        private const val MICROPHONE_QUERY = "/?speech=true"
    }
}
