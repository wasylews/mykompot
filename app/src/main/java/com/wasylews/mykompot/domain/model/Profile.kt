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

import java.time.LocalDate

/**
 * Account profile information retrieved from the portal.
 *
 * @property isActive whether the account/service is currently enabled (portal status `Enable`).
 * @property nextPaymentDate date of the next periodic payment, or `null` if unavailable.
 * @property tariffPlan the account's current tariff plan (resolved from catalog when possible).
 */
data class Profile(
    val isActive: Boolean,
    val nextPaymentDate: LocalDate?,
    val tariffPlan: TariffPlan,
) {
    /** Stable identifier of the current tariff plan. */
    val currentTariffPlanId: String get() = tariffPlan.id
}
