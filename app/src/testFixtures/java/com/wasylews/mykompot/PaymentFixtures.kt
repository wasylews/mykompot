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

import com.wasylews.mykompot.domain.model.Payment
import com.wasylews.mykompot.domain.model.PaymentSource
import java.time.LocalDateTime

object PaymentFixtures {
    fun payment(
        dateTime: LocalDateTime = LocalDateTime.of(2026, 10, 1, 10, 5),
        amount: Int = 300,
        tariffName: String = "Standart 100",
        source: PaymentSource = PaymentSource.PRIVAT24,
    ) = Payment(
        dateTime = dateTime,
        amount = amount,
        tariffName = tariffName,
        source = source,
    )
}
