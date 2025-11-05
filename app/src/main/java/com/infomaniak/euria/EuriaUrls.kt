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

import com.infomaniak.core.network.ApiEnvironment

private val host = ApiEnvironment.current.host

val CREATE_ACCOUNT_URL = "https://welcome.$host/signup/euria"
val UPGRADE_ACCOUNT_URL = "https://manager.$host/v3/mobile_login/?url=$CREATE_ACCOUNT_URL"

// iOS has a different URL for the success (ksuite.infomaniak.com) but for some reason, we don't have this
val CREATE_ACCOUNT_SUCCESS_HOST = "euria.$host"
val CREATE_ACCOUNT_CANCEL_HOST = "welcome.$host"

// val EURIA_MAIN_URL = "https://euria.$host"
val EURIA_MAIN_URL = "https://10.0.2.2:5174"
