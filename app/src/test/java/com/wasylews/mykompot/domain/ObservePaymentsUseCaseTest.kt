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

import com.wasylews.mykompot.domain.model.Payment
import com.wasylews.mykompot.domain.model.PaymentSource
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.presentation.FakePortalRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ObservePaymentsUseCaseTest {
    @Test
    fun `observes payments from repository`() = runTest {
        val payments = Payments(
            items = listOf(
                Payment(
                    dateTime = LocalDateTime.of(2026, 10, 1, 10, 5),
                    amount = 300,
                    tariffName = "Standart 100",
                    source = PaymentSource.PRIVAT24,
                ),
            ),
            hasMore = true,
        )
        val repository = FakePortalRepository(payments = payments)
        val useCase = ObservePaymentsUseCase(repository)

        val result = useCase().first()

        assertEquals(payments, result)
    }
}
