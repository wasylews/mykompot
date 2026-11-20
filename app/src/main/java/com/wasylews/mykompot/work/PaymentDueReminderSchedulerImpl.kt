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

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wasylews.mykompot.domain.PaymentDueReminderScheduler
import com.wasylews.mykompot.domain.model.PaymentDueThreshold
import com.wasylews.mykompot.domain.model.Profile
import org.koin.core.annotation.Singleton
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.toJavaInstant

@Singleton
class PaymentDueReminderSchedulerImpl(
    private val workManager: WorkManager,
    private val clock: Clock,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : PaymentDueReminderScheduler {
    override fun schedule(profile: Profile?) {
        workManager.cancelAllWorkByTag(REMINDER_TAG)

        val dueDate = profile?.takeIf { it.isActive }?.nextPaymentDate ?: return
        val now = now()
        remindersFor(dueDate, now).forEach { (threshold, targetTime) ->
            val delay = Duration.between(now, targetTime)
            if (!delay.isPositive) return@forEach

            val request = OneTimeWorkRequestBuilder<PaymentDueReminderWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        KEY_THRESHOLD to threshold.name,
                        KEY_DUE_DATE to dueDate.toString(),
                    ),
                )
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .addTag(REMINDER_TAG)
                .build()

            workManager.enqueueUniqueWork(
                workName(threshold, dueDate),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    override fun cancel() {
        workManager.cancelAllWorkByTag(REMINDER_TAG)
    }

    private fun now(): ZonedDateTime = clock.now().toJavaInstant().atZone(zoneId)

    private fun remindersFor(
        dueDate: LocalDate,
        now: ZonedDateTime,
    ): List<Pair<PaymentDueThreshold, ZonedDateTime>> {
        val reminderTime = LocalTime.of(REMINDER_HOUR, 0)
        val today = now.toLocalDate()

        if (dueDate.isBefore(today)) {
            val overdueTime = if (now.toLocalTime().isBefore(reminderTime)) {
                today.atTime(reminderTime).atZone(zoneId)
            } else {
                today.plusDays(1).atTime(reminderTime).atZone(zoneId)
            }
            return listOf(PaymentDueThreshold.OVERDUE to overdueTime)
        }

        return buildList {
            add(
                PaymentDueThreshold.THREE_DAYS_BEFORE to dueDate
                    .minusDays(PaymentDueThreshold.THREE_DAYS_BEFORE.daysBeforeDue)
                    .atTime(reminderTime).atZone(zoneId),
            )
            add(
                PaymentDueThreshold.ONE_DAY_BEFORE to dueDate
                    .minusDays(PaymentDueThreshold.ONE_DAY_BEFORE.daysBeforeDue)
                    .atTime(reminderTime).atZone(zoneId),
            )
            add(PaymentDueThreshold.DUE_DAY to dueDate.atTime(reminderTime).atZone(zoneId))
        }
    }

    companion object {
        const val REMINDER_TAG = "payment_due_reminder"
        const val REMINDER_HOUR = 9
        const val KEY_THRESHOLD = "threshold"
        const val KEY_DUE_DATE = "dueDate"

        fun workName(
            threshold: PaymentDueThreshold,
            dueDate: LocalDate,
        ): String = "payment_due_${threshold.name}_$dueDate"
    }
}
