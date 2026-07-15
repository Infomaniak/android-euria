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

package com.infomaniak.euria.webview

import com.infomaniak.euria.utils.extensions.escapeForJavascriptString
import com.infomaniak.euria.utils.extensions.isAllowedUpgradeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JavascriptBridgeSecurityTest {

    @Test
    fun `escapeForJavascriptString escapes executable delimiters`() {
        val query = """chat\"><script>\path""" + "\n"

        assertEquals(
            """chat\u005C\u0022\u003E\u003Cscript\u003E\u005Cpath\u000A""",
            query.escapeForJavascriptString(),
        )
    }

    @Test
    fun `escapeForJavascriptString preserves safe query characters`() {
        assertEquals("/chat?id=123&mode=voice", "/chat?id=123&mode=voice".escapeForJavascriptString())
    }

    @Test
    fun `isAllowedUpgradeUrl accepts any subdomain of infomaniak com`() {
        assertTrue("https://manager.infomaniak.com/v3/mobile_login?url=%2Fupgrade".isAllowedUpgradeUrl())
        assertTrue("https://shop.infomaniak.com/order".isAllowedUpgradeUrl())
    }

    @Test
    fun `isAllowedUpgradeUrl accepts any subdomain of infomaniak ch in preprod`() {
        assertTrue("https://manager.preprod.dev.infomaniak.ch/v3/upgrade".isAllowedUpgradeUrl())
    }

    @Test
    fun `isAllowedUpgradeUrl rejects untrusted URL variants`() {
        assertFalse("http://manager.infomaniak.com/upgrade".isAllowedUpgradeUrl())
        assertFalse("https://manager.infomaniak.com.evil.example/upgrade".isAllowedUpgradeUrl())
        assertFalse("https://manager.infomaniak.com@evil.example/upgrade".isAllowedUpgradeUrl())
        assertFalse("https://notinfomaniak.com/upgrade".isAllowedUpgradeUrl())
        assertFalse("not a url".isAllowedUpgradeUrl())
    }
}
