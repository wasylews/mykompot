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
import com.wasylews.mykompot.ProfileFixtures
import com.wasylews.mykompot.data.LocalStorage
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlans
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentDueReminderSchedulerTest : BaseWorkManagerSchedulerTest() {
    private val notifiedStore = FakePaymentDueNotifiedStore()
    private val localStorage = FakeLocalStorage()

    private fun scheduler() = PaymentDueReminderSchedulerImpl(workManager, localStorage, notifiedStore)

    @Test
    fun enqueuesOneDailyPeriodicWorkWhenProfileCanNeedReminders() = runTest {
        val job = launch { scheduler().start() }

        localStorage.saveProfile(dueProfile())
        advanceUntilIdle()

        assertDailyWork(WorkInfo.State.ENQUEUED)
        job.cancel()
    }

    @Test
    fun keepsSingleWorkWhenProfileChanges() = runTest {
        val job = launch { scheduler().start() }

        localStorage.saveProfile(dueProfile(LocalDate.parse("2026-10-04")))
        advanceUntilIdle()
        localStorage.saveProfile(dueProfile(LocalDate.parse("2026-11-01")))
        advanceUntilIdle()

        val infos = workManager.getWorkInfosForUniqueWork(PaymentDueReminderSchedulerImpl.DAILY_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        job.cancel()
    }

    @Test
    fun cancelsDailyWorkWhenProfileCannotNeedReminders() = runTest {
        val job = launch { scheduler().start() }

        listOf<Profile?>(
            null,
            dueProfile().copy(isActive = false),
            dueProfile(nextPaymentDate = null),
        ).forEach { invalidProfile ->
            localStorage.saveProfile(dueProfile())
            advanceUntilIdle()
            localStorage.setProfile(invalidProfile)
            advanceUntilIdle()

            assertDailyWork(WorkInfo.State.CANCELLED)
        }
        job.cancel()
    }

    @Test
    fun cancelsLegacyOneShotReminders() = runTest {
        val legacy = OneTimeWorkRequestBuilder<PaymentDueReminderWorker>()
            .setInitialDelay(10, TimeUnit.MINUTES)
            .addTag(PaymentDueReminderSchedulerImpl.LEGACY_REMINDER_TAG)
            .build()
        workManager.enqueue(legacy)
        val job = launch { scheduler().start() }

        localStorage.saveProfile(dueProfile())
        advanceUntilIdle()

        val infos = workManager.getWorkInfosByTag(PaymentDueReminderSchedulerImpl.LEGACY_REMINDER_TAG).get()
        assertTrue(infos.isNotEmpty())
        assertTrue(infos.all { it.state == WorkInfo.State.CANCELLED })
        job.cancel()
    }

    @Test
    fun clearsNotifiedLedgerWhenProfileCannotNeedReminders() = runTest {
        val job = launch { scheduler().start() }
        val ledgerKey = "DUE_DAY|2026-10-04"

        listOf<Profile?>(
            null,
            dueProfile().copy(isActive = false),
            dueProfile(nextPaymentDate = null),
        ).forEach { invalidProfile ->
            localStorage.saveProfile(dueProfile())
            advanceUntilIdle()
            notifiedStore.markNotified(ledgerKey)
            assertTrue(notifiedStore.isNotified(ledgerKey))

            localStorage.setProfile(invalidProfile)
            advanceUntilIdle()

            assertFalse(notifiedStore.isNotified(ledgerKey))
            assertDailyWork(WorkInfo.State.CANCELLED)
        }
        job.cancel()
    }

    @Test
    fun doesNotCreateWorkWhenNoProfileIsStoredAtStartup() = runTest {
        val job = launch { scheduler().start() }

        advanceUntilIdle()

        val infos = workManager.getWorkInfosForUniqueWork(PaymentDueReminderSchedulerImpl.DAILY_WORK_NAME).get()
        assertTrue(infos.isEmpty())
        job.cancel()
    }

    private suspend fun FakeLocalStorage.setProfile(profile: Profile?) {
        if (profile == null) clearProfile() else saveProfile(profile)
    }

    private fun dueProfile(nextPaymentDate: LocalDate? = LocalDate.parse("2026-10-04")) =
        ProfileFixtures.profile(nextPaymentDate = nextPaymentDate)

    private fun assertDailyWork(state: WorkInfo.State) {
        val infos = workManager.getWorkInfosForUniqueWork(PaymentDueReminderSchedulerImpl.DAILY_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(state, infos.single().state)
        assertTrue(infos.single().tags.contains(PaymentDueReminderSchedulerImpl.REMINDER_TAG))
    }
}

private class FakeLocalStorage : LocalStorage {
    private val profile = MutableStateFlow<Profile?>(null)

    override suspend fun saveProfile(profile: Profile) {
        this.profile.value = profile
    }

    override fun profileFlow(): Flow<Profile?> = profile.asStateFlow()

    override suspend fun clearProfile() {
        profile.value = null
    }

    override fun tariffPlansFlow(): Flow<TariffPlans?> = MutableStateFlow(null)

    override suspend fun saveTariffPlans(tariffPlans: TariffPlans) = Unit

    override fun paymentsFlow(): Flow<Payments?> = MutableStateFlow(null)

    override suspend fun savePayments(payments: Payments) = Unit

    override suspend fun clearPayments() = Unit
}
