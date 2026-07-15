/*
 * Infomaniak Euria - Android
 * Copyright (C) 2026 Infomaniak Network SA
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
package com.infomaniak.euria.utils.extensions

import java.net.URI
import java.util.Locale

// Allow any subdomain of infomaniak.com (prod) or infomaniak.ch (dev/preprod).
private val ALLOWED_DOMAINS = setOf("infomaniak.com", "infomaniak.ch")

private fun Char.shouldEscapeForJavascript(): Boolean {
    val dangerousChars = setOf('\\', '"', '<', '>')
    return this in dangerousChars ||
            this.code in 0x00..0x1f ||
            this == '\u2028' ||
            this == '\u2029'
}

fun String.escapeForJavascriptString(): String = buildString(length) {
    this@escapeForJavascriptString.forEach { character ->
        when {
            character.shouldEscapeForJavascript() -> {
                append("\\u")
                append(character.code.toString(radix = 16).uppercase(Locale.ROOT).padStart(4, '0'))
            }
            else -> append(character)
        }
    }
}

fun String.isAllowedUpgradeUrl(): Boolean {
    val uri = runCatching { URI(this) }.getOrNull() ?: return false
    val normalizedHost = uri.host?.lowercase(Locale.ROOT) ?: return false

    val isAllowedDomain = ALLOWED_DOMAINS.any { domain ->
        normalizedHost == domain || normalizedHost.endsWith(".$domain")
    }

    return uri.scheme.equals("https", ignoreCase = true) && isAllowedDomain
}
