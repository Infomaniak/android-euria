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
import com.infomaniak.euria.MainActivity.Companion.EXTRA_ACTION
import com.infomaniak.euria.MainActivity.Companion.EXTRA_QUERY
import com.infomaniak.euria.MainActivity.PendingIntentRequestCodes
import com.infomaniak.euria.R

class EuriaAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) return

        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        views.setOnClickPendingIntent(
            R.id.newConversationButton,
            getBroadcastPendingIntent(
                context = context,
                requestCode = PendingIntentRequestCodes.CHAT,
            )
        )
        views.setOnClickPendingIntent(
            R.id.ephemeralButton,
            getBroadcastPendingIntent(
                context = context,
                requestCode = PendingIntentRequestCodes.EPHEMERAL,
                query = EPHEMERAL_QUERY,
            )
        )
        views.setOnClickPendingIntent(
            R.id.microphoneButton,
            getBroadcastPendingIntent(
                context = context,
                requestCode = PendingIntentRequestCodes.SPEECH,
                query = MICROPHONE_QUERY,
            )
        )
        views.setOnClickPendingIntent(
            R.id.cameraButton,
            getBroadcastPendingIntent(
                context = context,
                requestCode = PendingIntentRequestCodes.CAMERA,
                actionValue = EXTRA_ACTION_CAMERA,
            )
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getBroadcastPendingIntent(
        context: Context,
        requestCode: Int,
        query: String? = null,
        actionValue: String? = null
    ): PendingIntent {
        val intent = Intent(context, WidgetClickReceiver::class.java).apply {
            action = requestCode.toString()

            putExtra(EXTRA_QUERY, query)
            putExtra(EXTRA_ACTION, actionValue)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_ACTION_CAMERA = "CAMERA"

        private const val EPHEMERAL_QUERY = "/?ephemeral=true"
        private const val MICROPHONE_QUERY = "/?speech=true"
    }
}
