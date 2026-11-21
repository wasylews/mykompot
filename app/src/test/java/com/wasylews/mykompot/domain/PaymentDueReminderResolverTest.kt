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

package com.wasylews.mykompot.domain

import com.wasylews.mykompot.ProfileFixtures
import com.wasylews.mykompot.domain.model.PaymentDueReminder
import com.wasylews.mykompot.domain.model.PaymentDueThreshold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PaymentDueReminderResolverTest {
    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun `resolves three-day heads-up`() {
        val reminder = PaymentDueReminderResolver.resolve(ProfileFixtures.profile(nextPaymentDate = today.plusDays(3)), today)

        assertEquals(PaymentDueThreshold.THREE_DAYS_BEFORE, reminder?.threshold)
        assertEquals(today.plusDays(3), reminder?.dueDate)
    }

    @Test
    fun `resolves due-day reminder`() {
        val reminder = PaymentDueReminderResolver.resolve(ProfileFixtures.profile(nextPaymentDate = today), today)

        assertEquals(PaymentDueThreshold.DUE_DAY, reminder?.threshold)
        assertEquals(today, reminder?.dueDate)
    }

    @Test
    fun `does not resolve one-day-before or overdue`() {
        assertNull(PaymentDueReminderResolver.resolve(ProfileFixtures.profile(nextPaymentDate = today.plusDays(1)), today))
        assertNull(PaymentDueReminderResolver.resolve(ProfileFixtures.profile(nextPaymentDate = today.minusDays(1)), today))
    }

    @Test
    fun `does not resolve outside the two reminder days`() {
        assertNull(PaymentDueReminderResolver.resolve(ProfileFixtures.profile(nextPaymentDate = today.plusDays(2)), today))
        assertNull(PaymentDueReminderResolver.resolve(ProfileFixtures.profile(nextPaymentDate = today.plusDays(4)), today))
    }

    @Test
    fun `does not resolve without an active profile and due date`() {
        assertNull(PaymentDueReminderResolver.resolve(null, today))
        assertNull(PaymentDueReminderResolver.resolve(ProfileFixtures.profile(nextPaymentDate = null), today))
        assertNull(PaymentDueReminderResolver.resolve(ProfileFixtures.profile(nextPaymentDate = today).copy(isActive = false), today))
    }

    @Test
    fun `ledger key includes threshold and due date`() {
        val reminder = PaymentDueReminder(PaymentDueThreshold.THREE_DAYS_BEFORE, today.plusDays(3))

        assertEquals("THREE_DAYS_BEFORE|2026-10-04", reminder.ledgerKey)
    }
}
