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

import com.infomaniak.core.auth.CredentialManager
import com.infomaniak.core.auth.TokenAuthenticator
import com.infomaniak.core.auth.models.user.User
import com.infomaniak.core.auth.networking.AuthHttpClientProvider
import com.infomaniak.core.auth.room.UserDatabase
import com.infomaniak.core.network.models.ApiResponseStatus
import com.infomaniak.euria.MainApplication
import com.infomaniak.euria.network.ApiRepository
import io.sentry.Sentry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import io.sentry.protocol.User as SentryUser

object AccountUtils : CredentialManager() {

    private const val DEFAULT_USER_ID = -1

    override lateinit var userDatabase: UserDatabase

    fun init() {
        userDatabase = UserDatabase.getDatabase()

        Sentry.setUser(SentryUser().apply { id = currentUserId.toString() })
    }

    override var currentUser: User? = null
        set(user) {
            field = user
            currentUserId = user?.id ?: DEFAULT_USER_ID
            Sentry.setUser(SentryUser().apply {
                id = currentUserId.toString()
                email = user?.email
            })
        }

    override var currentUserId: Int = currentUser?.id ?: DEFAULT_USER_ID

    suspend fun requestCurrentUser(): User? {
        val user = currentUserId.takeIf { it != DEFAULT_USER_ID }?.let {
            getUserById(currentUserId)
        }
        return (user ?: userDatabase.userDao().getFirst()).also { currentUser = it }
    }

    fun getCurrentUserFlow(): Flow<User?> = userDatabase.userDao().getFirstFlow()

    suspend fun addUser(user: User) {
        currentUser = user
        val userId = user.id.toLong()
        MainApplication.userDataCleanableList.forEach { it.resetForUser(userId) }
        userDatabase.userDao().insert(user)
    }

    suspend fun updateCurrentUser(okHttpClient: OkHttpClient = AuthHttpClientProvider.authOkHttpClient) {
        with(ApiRepository.getUserProfile(okHttpClient)) {
            if (result != ApiResponseStatus.ERROR) requestUser(remoteUser = data ?: return)
        }
    }

    suspend fun removeAllUser() {
        userDatabase.userDao().getFirst()?.let { user ->
            val userId = user.id.toLong()
            MainApplication.userDataCleanableList.forEach { it.resetForUser(userId) }
            userDatabase.userDao().delete(user)
        }
    }

    private suspend fun requestUser(remoteUser: User) {
        TokenAuthenticator.mutex.withLock {
            if (remoteUser.id == currentUserId) {
                remoteUser.organizations = arrayListOf()
                requestCurrentUser()?.let { localUser ->
                    setUserToken(remoteUser, localUser.apiToken)
                }
            }
        }
    }
}
