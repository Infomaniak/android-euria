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
package com.infomaniak.euria

import android.content.Intent
import com.infomaniak.core.crossapplogin.back.BaseCrossAppLoginService
import kotlinx.coroutines.flow.MutableStateFlow

class CrossAppLoginService : BaseCrossAppLoginService(userIdFlow) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getIntExtra(EXTRA_USER_ID, -1)
        if (userId != null) userIdFlow.value = userId

        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        const val EXTRA_USER_ID = "userId"

        private val userIdFlow = MutableStateFlow<Int?>(null)
    }
}
