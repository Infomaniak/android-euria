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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.infomaniak.euria.MainActivity
import com.infomaniak.euria.MatomoEuria

class WidgetClickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val matomoName = intent.action?.let { getMatomoNameFromAction(it) }
        matomoName?.let { MatomoEuria.trackWidgetEvent(it) }

        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val extras = intent.extras
            if (extras != null) putExtras(extras)
        }
        context.startActivity(mainActivityIntent)
    }

    private fun getMatomoNameFromAction(action: String): MatomoEuria.MatomoName? {
        return when (action) {
            MainActivity.PendingIntentRequestCodes.CHAT.toString() -> MatomoEuria.MatomoName.NewChat
            MainActivity.PendingIntentRequestCodes.EPHEMERAL.toString() -> MatomoEuria.MatomoName.EphemeralMode
            MainActivity.PendingIntentRequestCodes.SPEECH.toString() -> MatomoEuria.MatomoName.EnableMicrophone
            MainActivity.PendingIntentRequestCodes.CAMERA.toString() -> MatomoEuria.MatomoName.OpenCamera
            else -> null
        }
    }
}
