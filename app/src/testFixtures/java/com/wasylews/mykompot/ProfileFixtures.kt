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

package com.wasylews.mykompot

import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import java.time.LocalDate

object ProfileFixtures {
    fun tariffPlan(): TariffPlan = TariffPlan(
        id = "300GRN_100mb",
        name = "Standart 100",
        speed = 100,
        price = 300,
    )

    fun tariffPlans(): List<TariffPlan> = listOf(
        TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
        tariffPlan(),
        TariffPlan(id = "350GRN_300mb", name = "LUX 300", speed = 300, price = 350),
    )

    fun profile(
        nextPaymentDate: LocalDate? = LocalDate.of(2026, 11, 1),
        isActive: Boolean = true,
        plan: TariffPlan = tariffPlan(),
    ): Profile = Profile(
        isActive = isActive,
        nextPaymentDate = nextPaymentDate,
        tariffPlan = plan,
    )

    fun allGoodProfile(): Profile = profile()

    fun paymentDueProfile(): Profile = profile(nextPaymentDate = LocalDate.of(2026, 10, 2))
}
