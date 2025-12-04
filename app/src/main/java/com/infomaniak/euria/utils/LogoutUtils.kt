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

package com.infomaniak.euria.utils

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.infomaniak.core.auth.models.user.User
import com.infomaniak.core.cancellable
import com.infomaniak.core.network.networking.DefaultHttpClientProvider
import com.infomaniak.core.sentry.SentryLog
import com.infomaniak.euria.data.LocalSettings
import com.infomaniak.euria.di.IoDispatcher
import com.infomaniak.euria.utils.extensions.getInfomaniakLogin
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogoutUtils @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val localSettings: LocalSettings,
    private val cookieManager: CookieManager,
    private val globalCoroutineScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun logout(user: User) {
        globalCoroutineScope.launch(ioDispatcher) {
            user.logoutToken()
            cookieManager.removeAllCookies(null)
            Dispatchers.IO.invoke { cookieManager.flush() }
            WebStorage.getInstance().deleteAllData()
            AccountUtils.removeAllUser()
            localSettings.removeSettings()
        }
    }

    private suspend fun User.logoutToken() {
        runCatching {
            appContext.getInfomaniakLogin().deleteToken(
                okHttpClient = DefaultHttpClientProvider.okHttpClient,
                token = apiToken,
            )?.let { errorStatus ->
                SentryLog.i(TAG, "API response error: $errorStatus")
            }
        }.cancellable().onFailure { exception ->
            SentryLog.e(TAG, "Failure on logoutToken ", exception)
        }
    }

    companion object {
        private val TAG = LogoutUtils::class.java.simpleName
    }
}
