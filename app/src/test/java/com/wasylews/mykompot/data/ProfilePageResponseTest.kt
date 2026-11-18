/*
 * Copyright 2026 Vasyl Sabov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wasylews.mykompot.data

import com.wasylews.mykompot.HtmlFixtures
import dev.burnoo.kspoon.Kspoon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePageResponseTest {
    @Test
    fun `parsing profile page`() {
        val page = HtmlFixtures.profile()
        val result = Kspoon.parse<ProfilePageResponse>(page)

        assertEquals("Enable", result.accountState)
        assertEquals("[35] 300GRN_100mb", result.plan)
        assertEquals("Next periodic payments throught days (2026-11-01)", result.nextPayment)
    }

    @Test
    fun `parsing suspended page`() {
        val page = HtmlFixtures.suspended()
        val result = Kspoon.parse<ProfilePageResponse>(page)

        assertEquals("Too Small Deposit", result.accountState)
        assertEquals("[35] 300GRN_100mb", result.plan)
        assertEquals("", result.nextPayment)
    }

    @Test
    fun `parsing payment due page`() {
        val page = HtmlFixtures.paymentDue()
        val result = Kspoon.parse<ProfilePageResponse>(page)

        assertEquals("Enable", result.accountState)
        assertEquals("[35] 300GRN_100mb", result.plan)
        assertEquals("Next periodic payments throught days (2026-10-02)", result.nextPayment)
    }

    @Test
    fun `parsing failed login page`() {
        val page = HtmlFixtures.loginFailed()
        val result = Kspoon.parse<ProfilePageResponse>(page)

        assertEquals("Error", result.error)
        assertTrue(result.isLoginPage)
    }

    @Test
    fun `generic portal error is not a login page`() {
        val page = HtmlFixtures.portalError()
        val result = Kspoon.parse<ProfilePageResponse>(page)

        assertEquals("Error", result.error)
        assertFalse(result.isLoginPage)
    }
}
