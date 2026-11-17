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

package com.wasylews.mykompot.work

import androidx.work.WorkInfo
import com.wasylews.mykompot.MutableClock
import com.wasylews.mykompot.domain.model.PaymentDueThreshold
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

class PaymentDueReminderSchedulerTest : BaseWorkManagerSchedulerTest() {
    @Test
    fun schedulesThreeDayOneDayAndDueRemindersForUpcomingPayment() {
        // 2026-10-01T07:00:00Z = 07:00 UTC, before 09:00 local (UTC)
        val clock = MutableClock(Instant.parse("2026-10-01T07:00:00Z"))
        val scheduler = PaymentDueReminderSchedulerImpl(workManager, clock, ZoneId.of("UTC"))

        scheduler.schedule(
            profile(nextPaymentDate = LocalDate.parse("2026-10-04")),
        )

        assertEnqueued(PaymentDueThreshold.THREE_DAYS_BEFORE, "2026-10-04")
        assertEnqueued(PaymentDueThreshold.ONE_DAY_BEFORE, "2026-10-04")
        assertEnqueued(PaymentDueThreshold.DUE_DAY, "2026-10-04")
    }

    @Test
    fun skipsRemindersWhoseTimeHasPassed() {
        // 2026-10-01T10:00:00Z, after 09:00 UTC on 3-days-before day
        val clock = MutableClock(Instant.parse("2026-10-01T10:00:00Z"))
        val scheduler = PaymentDueReminderSchedulerImpl(workManager, clock, ZoneId.of("UTC"))

        scheduler.schedule(
            profile(nextPaymentDate = LocalDate.parse("2026-10-04")),
        )

        assertNotEnqueued(PaymentDueThreshold.THREE_DAYS_BEFORE, "2026-10-04")
        assertEnqueued(PaymentDueThreshold.ONE_DAY_BEFORE, "2026-10-04")
        assertEnqueued(PaymentDueThreshold.DUE_DAY, "2026-10-04")
    }

    @Test
    fun schedulesOverdueReminderWhenPaymentIsPastDue() {
        // 2026-10-05T07:00:00Z, due was 2026-10-04 (overdue), before 09:00 today
        val clock = MutableClock(Instant.parse("2026-10-05T07:00:00Z"))
        val scheduler = PaymentDueReminderSchedulerImpl(workManager, clock, ZoneId.of("UTC"))

        scheduler.schedule(
            profile(nextPaymentDate = LocalDate.parse("2026-10-04")),
        )

        assertEnqueued(PaymentDueThreshold.OVERDUE, "2026-10-04")
        assertNotEnqueued(PaymentDueThreshold.THREE_DAYS_BEFORE, "2026-10-04")
        assertNotEnqueued(PaymentDueThreshold.ONE_DAY_BEFORE, "2026-10-04")
        assertNotEnqueued(PaymentDueThreshold.DUE_DAY, "2026-10-04")
    }

    @Test
    fun cancelsRemindersWhenProfileIsMissingOrInactiveOrHasNoDueDate() {
        val clock = MutableClock(Instant.parse("2026-10-01T07:00:00Z"))
        val scheduler = PaymentDueReminderSchedulerImpl(workManager, clock, ZoneId.of("UTC"))

        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")))
        scheduler.schedule(null)
        assertNoReminders("2026-10-04")

        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")))
        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")).copy(isActive = false))
        assertNoReminders("2026-10-04")

        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")))
        scheduler.schedule(profile(nextPaymentDate = null))
        assertNoReminders("2026-10-04")
    }

    @Test
    fun reschedulesWhenDueDateChanges() {
        val clock = MutableClock(Instant.parse("2026-10-01T07:00:00Z"))
        val scheduler = PaymentDueReminderSchedulerImpl(workManager, clock, ZoneId.of("UTC"))

        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")))
        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-11-01")))

        assertNoReminders("2026-10-04")
        assertEnqueued(PaymentDueThreshold.THREE_DAYS_BEFORE, "2026-11-01")
        assertEnqueued(PaymentDueThreshold.ONE_DAY_BEFORE, "2026-11-01")
        assertEnqueued(PaymentDueThreshold.DUE_DAY, "2026-11-01")
    }

    private fun assertEnqueued(
        threshold: PaymentDueThreshold,
        dueDate: String,
    ) {
        val infos = workManager.getWorkInfosForUniqueWork(
            PaymentDueReminderSchedulerImpl.workName(threshold, LocalDate.parse(dueDate)),
        ).get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
    }

    private fun assertNotEnqueued(
        threshold: PaymentDueThreshold,
        dueDate: String,
    ) {
        val infos = workManager.getWorkInfosForUniqueWork(
            PaymentDueReminderSchedulerImpl.workName(threshold, LocalDate.parse(dueDate)),
        ).get()
        assertTrue(infos.isEmpty() || infos.none { it.state == WorkInfo.State.ENQUEUED })
    }

    private fun assertNoReminders(dueDate: String) {
        PaymentDueThreshold.entries.forEach { threshold ->
            assertNotEnqueued(threshold, dueDate)
        }
    }

    private fun profile(nextPaymentDate: LocalDate?) = Profile(
        isActive = true,
        nextPaymentDate = nextPaymentDate,
        tariffPlan = TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
    )
}
