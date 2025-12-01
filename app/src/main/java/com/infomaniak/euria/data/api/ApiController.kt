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

package com.infomaniak.euria.data.api

import com.infomaniak.core.network.api.ApiController
import com.infomaniak.core.network.models.ApiResponse
import com.infomaniak.euria.data.models.UploadFile
import okhttp3.MultipartBody
import okhttp3.OkHttpClient

object ApiController {

    suspend inline fun <reified T> callApi(
        url: String,
        method: ApiController.ApiMethod,
        body: Any? = null,
        okHttpClient: OkHttpClient,
    ): T = ApiController.callApi(url, method, body, okHttpClient, useKotlinxSerialization = true)

    suspend fun uploadFile(
        okHttpClient: OkHttpClient,
        organizationId: String,
        fileAsByteArray: MultipartBody
    ): ApiResponse<UploadFile> {
        return callApi(
            url = ApiRoutes.uploadFile(organizationId),
            method = ApiController.ApiMethod.POST,
            body = fileAsByteArray,
            okHttpClient = okHttpClient,
        )
    }
}
