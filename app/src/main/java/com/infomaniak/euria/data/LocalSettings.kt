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

package com.infomaniak.euria.data

import android.content.Context
import com.infomaniak.core.sharedvalues.SharedValues
import com.infomaniak.core.sharedvalues.transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSettings @Inject constructor(@ApplicationContext context: Context) : SharedValues {

    override val sharedPreferences = context.applicationContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)!!

    var wantToDiscoverEuria by sharedValue("wantToDiscoverEuria", false)

    fun removeSettings() = sharedPreferences.transaction { clear() }

    companion object {
        private const val SHARED_PREFS_NAME = "LocalSettingsSharedPref"
    }
}
