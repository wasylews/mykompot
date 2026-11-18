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

package com.wasylews.mykompot.domain.model

/**
 * Derived account status used by the UI and reminders.
 *
 * @property state high-level status derived from [profile] and time until the next payment.
 * @property profile the underlying account profile.
 * @property daysLeft days between today and [Profile.nextPaymentDate], or `null` when unknown.
 */
data class AccountStatus(
    val state: State,
    val profile: Profile,
    val daysLeft: Long?,
) {
    /** High-level account state categories. */
    enum class State {
        /** Account is active and no payment is due within the configured window. */
        ALL_GOOD,

        /** Account is active but a payment is due soon, today, or overdue by date. */
        PAYMENT_DUE,

        /** Account/service is not active. */
        SUSPENDED,
    }
}
