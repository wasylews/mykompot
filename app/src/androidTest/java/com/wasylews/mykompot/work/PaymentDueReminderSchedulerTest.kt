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

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import com.wasylews.mykompot.FakePaymentDueNotifiedStore
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class PaymentDueReminderSchedulerTest : BaseWorkManagerSchedulerTest() {
    private val notifiedStore = FakePaymentDueNotifiedStore()

    private fun scheduler() = PaymentDueReminderSchedulerImpl(workManager, notifiedStore)

    @Test
    fun enqueuesOneDailyPeriodicWork() {
        scheduler().schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")))

        assertDailyWork(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun repeatedScheduleKeepsSingleWork() {
        val scheduler = scheduler()
        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")))
        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-11-01")))

        val infos = workManager.getWorkInfosForUniqueWork(PaymentDueReminderSchedulerImpl.DAILY_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
    }

    @Test
    fun cancelsDailyWorkWhenProfileCannotNeedReminders() {
        val scheduler = scheduler()
        listOf(
            null,
            profile(nextPaymentDate = LocalDate.parse("2026-10-04")).copy(isActive = false),
            profile(nextPaymentDate = null),
        ).forEach { invalidProfile ->
            scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")))
            scheduler.schedule(invalidProfile)
            assertDailyWork(WorkInfo.State.CANCELLED)
        }
    }

    @Test
    fun cancelsLegacyOneShotReminders() {
        val legacy = OneTimeWorkRequestBuilder<PaymentDueReminderWorker>()
            .setInitialDelay(10, TimeUnit.MINUTES)
            .addTag(PaymentDueReminderSchedulerImpl.LEGACY_REMINDER_TAG)
            .build()
        workManager.enqueue(legacy)

        scheduler().schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-04")))

        val infos = workManager.getWorkInfosByTag(PaymentDueReminderSchedulerImpl.LEGACY_REMINDER_TAG).get()
        assertTrue(infos.isNotEmpty())
        assertTrue(infos.all { it.state == WorkInfo.State.CANCELLED })
    }

    @Test
    fun cancelClearsNotifiedLedger() = runBlocking {
        val scheduler = scheduler()
        scheduler.schedule(profile(nextPaymentDate = LocalDate.parse("2026-10-01")))
        notifiedStore.markNotified("DUE_DAY|2026-10-01")

        scheduler.cancel()

        assertDailyWork(WorkInfo.State.CANCELLED)
        assertFalse(notifiedStore.isNotified("DUE_DAY|2026-10-01"))
    }

    private fun assertDailyWork(state: WorkInfo.State) {
        val infos = workManager.getWorkInfosForUniqueWork(PaymentDueReminderSchedulerImpl.DAILY_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(state, infos.single().state)
        assertTrue(infos.single().tags.contains(PaymentDueReminderSchedulerImpl.REMINDER_TAG))
    }

    private fun profile(nextPaymentDate: LocalDate?) = Profile(
        isActive = true,
        nextPaymentDate = nextPaymentDate,
        tariffPlan = TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
    )
}
