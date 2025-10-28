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
import androidx.core.content.edit
import com.infomaniak.core.sharedvalues.SharedValues
import com.infomaniak.core.sharedvalues.sharedValue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSharedPref @Inject constructor(@ApplicationContext context: Context) : SharedValues {

    override val sharedPreferences = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)!!

    var token: String? by sharedValue(TOKEN_KEY, null)
    var userId: Int by sharedValue(USER_ID_KEY, -1)
    var avatarUrl: String? by sharedValue(AVATAR_URL_KEY, null)
    var fullName: String by sharedValue(FULL_NAME_KEY, "")
    var initials: String by sharedValue(INITIALS_KEY, "")
    var email: String by sharedValue(EMAIL_KEY, "")

    fun deleteUserInfo() {
        sharedPreferences.edit {
            clear()
            apply()
        }
    }

    companion object {
        const val NAME = "com.infomaniak.euria.usersharedpref"

        private const val TOKEN_KEY = "com.infomaniak.euria.usersharedpref.token"
        private const val USER_ID_KEY = "com.infomaniak.euria.usersharedpref.user_id"
        private const val AVATAR_URL_KEY = "com.infomaniak.euria.usersharedpref.avatar_url"
        private const val FULL_NAME_KEY = "com.infomaniak.euria.usersharedpref.full_name"
        private const val INITIALS_KEY = "com.infomaniak.euria.usersharedpref.initials"
        private const val EMAIL_KEY = "com.infomaniak.euria.usersharedpref.email"
    }
}
