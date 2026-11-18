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

import java.time.LocalDateTime

/**
 * A single payment/fee entry from the portal history.
 *
 * @property dateTime when the payment was recorded.
 * @property amount payment amount in UAH.
 * @property tariffName plan name associated with the payment amount when known, otherwise a
 * fallback label derived from the amount.
 * @property source payment channel inferred from the portal description.
 */
data class Payment(
    val dateTime: LocalDateTime,
    val amount: Int,
    val tariffName: String,
    val source: PaymentSource,
)

/** Known payment sources/channels recognized from portal payment descriptions. */
enum class PaymentSource {
    PRIVAT24,
    EASYPAY,
    MONOBANK,
    UNKNOWN,
}
