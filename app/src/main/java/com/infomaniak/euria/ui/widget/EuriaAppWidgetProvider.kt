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
import android.widget.RemoteViews
import com.infomaniak.euria.MainActivity
import com.infomaniak.euria.MainActivity.Companion.EXTRA_QUERY
import com.infomaniak.euria.R

class EuriaAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // We only have on widget so we use the first one
        val appWidgetId = appWidgetIds.firstOrNull() ?: return

        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val mainIntent = getIntent(context)
        val ephemeralIntent = getIntent(context, query = EPHEMERAL_QUERY)
        val microphoneIntent = getIntent(context, query = MICROPHONE_QUERY)

        views.setOnClickPendingIntent(R.id.newConversationButton, getPendingIntent(context, mainIntent))
        views.setOnClickPendingIntent(R.id.ephemeralButton, getPendingIntent(context, ephemeralIntent))
        views.setOnClickPendingIntent(R.id.microphoneButton, getPendingIntent(context, microphoneIntent))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getIntent(context: Context, query: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_QUERY, query)
        }
    }

    private fun getPendingIntent(context: Context, intent: Intent): PendingIntent {
        return PendingIntent.getActivity(
            context,
            intent.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val EPHEMERAL_QUERY = "/?ephemeral=true"
        private const val MICROPHONE_QUERY = "/?speech=true"
    }
}
