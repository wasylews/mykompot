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

import com.wasylews.mykompot.domain.model.PaymentDueReminder
import com.wasylews.mykompot.domain.model.PaymentDueThreshold
import com.wasylews.mykompot.domain.model.Profile
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Decides which payment-due notice, if any, applies to the cached profile right now.
 *
 * Only the heads-up and due-day notices are used. The decision is made at execution time so it
 * survives time and timezone changes without rescheduling work.
 */
object PaymentDueReminderResolver {
    fun resolve(
        profile: Profile?,
        today: LocalDate,
    ): PaymentDueReminder? {
        val dueDate = profile?.takeIf { it.isActive }?.nextPaymentDate ?: return null
        val daysLeft = ChronoUnit.DAYS.between(today, dueDate)

        val threshold = when (daysLeft) {
            PaymentDueThreshold.THREE_DAYS_BEFORE.daysBeforeDue -> PaymentDueThreshold.THREE_DAYS_BEFORE
            PaymentDueThreshold.DUE_DAY.daysBeforeDue -> PaymentDueThreshold.DUE_DAY
            else -> return null
        }
        return PaymentDueReminder(threshold, dueDate)
    }
}
