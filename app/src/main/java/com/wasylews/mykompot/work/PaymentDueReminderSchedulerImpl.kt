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

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wasylews.mykompot.data.LocalStorage
import com.wasylews.mykompot.domain.PaymentDueNotifiedStore
import com.wasylews.mykompot.domain.PaymentDueReminderScheduler
import com.wasylews.mykompot.domain.model.Profile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Singleton
import java.util.concurrent.TimeUnit

/**
 * Keeps one daily periodic check alive instead of scheduling fixed-time one-shot reminders.
 *
 * The observer follows the cached profile, so login, refresh, session expiry and logout need no
 * explicit scheduling calls. The worker re-evaluates the cached profile when it runs, so time and
 * timezone changes need no rescheduling and the work survives process death and reboot.
 */
@Singleton
class PaymentDueReminderSchedulerImpl(
    private val workManager: WorkManager,
    private val localStorage: LocalStorage,
    private val notifiedStore: PaymentDueNotifiedStore,
) : PaymentDueReminderScheduler {
    override suspend fun start() {
        localStorage.profileFlow()
            .map(::needsReminders)
            .distinctUntilChanged()
            .collect { needed ->
                removeLegacyWork()
                if (needed) {
                    enqueueDailyWork()
                } else {
                    workManager.cancelUniqueWork(DAILY_WORK_NAME)
                    notifiedStore.clear()
                }
            }
    }

    private fun needsReminders(profile: Profile?): Boolean = profile?.isActive == true && profile.nextPaymentDate != null

    private fun enqueueDailyWork() {
        val request = PeriodicWorkRequestBuilder<PaymentDueReminderWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS,
            FLEX_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .addTag(REMINDER_TAG)
            .build()

        // KEEP avoids extending the current period on every profile refresh.
        workManager.enqueueUniquePeriodicWork(DAILY_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun removeLegacyWork() {
        workManager.cancelAllWorkByTag(LEGACY_REMINDER_TAG)
    }

    companion object {
        const val DAILY_WORK_NAME = "payment_due_daily"
        const val REMINDER_TAG = "payment_due_daily_reminder"
        const val LEGACY_REMINDER_TAG = "payment_due_reminder"
        const val REPEAT_INTERVAL_HOURS = 24L
        const val FLEX_INTERVAL_HOURS = 1L
    }
}
