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

import com.infomaniak.core.auth.TokenAuthenticator.Companion.changeAccessToken
import com.infomaniak.core.auth.networking.AuthHttpClientProvider
import okhttp3.OkHttpClient

object OkHttpClientProvider {

    fun getOkHttpClientProvider(token: String): OkHttpClient {
        return when {
            token.isNotEmpty() -> {
                AuthHttpClientProvider.authOkHttpClient.newBuilder().addInterceptor { chain ->
                    val newRequest = changeAccessToken(chain.request(), token)
                    chain.proceed(newRequest)
                }.build()
            }
            else -> AuthHttpClientProvider.authOkHttpClient
        }
    }
}
