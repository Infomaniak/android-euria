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

package com.infomaniak.euria.di

import com.infomaniak.core.twofactorauth.back.ConnectionAttemptInfo
import com.infomaniak.core.twofactorauth.back.TwoFactorAuthManager
import com.infomaniak.euria.data.UserSharedPref
import com.infomaniak.euria.utils.OkHttpClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.flowOf

@Module
@InstallIn(SingletonComponent::class)
object TwoFactorAuthManagerModule {

    @Provides
    fun providesTwoFactorAuthManager(
        userSharedPref: UserSharedPref,
    ): TwoFactorAuthManager {
        val targetAccount = with(userSharedPref) {
            ConnectionAttemptInfo.TargetAccount(
                avatarUrl = avatarUrl,
                fullName = fullName,
                initials = initials,
                email = email,
                id = userId.toLong(),
            )
        }

        return TwoFactorAuthManager(
            userIds = flowOf(setOf(targetAccount.id.toInt())),
            getAccountInfo = { targetAccount },
            getConnectedHttpClient = { OkHttpClientProvider.getOkHttpClientProvider(userSharedPref.token) }
        )
    }
}
