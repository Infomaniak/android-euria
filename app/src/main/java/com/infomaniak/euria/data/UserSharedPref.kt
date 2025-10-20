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
import android.content.SharedPreferences
import androidx.core.content.edit

object UserSharedPref {

    private const val NAME = "com.infomaniak.euria.usersharedpref"
    private const val TOKEN_KEY = "com.infomaniak.euria.usersharedpref.token"
    private const val USER_ID_KEY = "com.infomaniak.euria.usersharedpref.user_id"

    fun Context.getToken() = getSharedPref(this).getString(TOKEN_KEY, null)

    fun Context.saveToken(token: String) {
        getSharedPref(this).save(TOKEN_KEY, token)
    }

    fun Context.getUserId() = getSharedPref(this).getString(USER_ID_KEY, null)

    fun Context.saveUserId(userId: Int) {
        getSharedPref(this).save(USER_ID_KEY, userId)
    }

    private fun getSharedPref(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    private fun <T> SharedPreferences.save(key: String, value: T) {
        edit {
            when (value) {
                is String -> putString(key, value as String)
                is Int -> putInt(key, value as Int)
                else -> throw IllegalArgumentException("Type not supported")
            }
        }
    }
}
