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

import app.cash.turbine.test
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.presentation.FakePortalRepository
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ObserveStatusUseCaseTest {
    @Test
    fun `active profile outside due window is all good`() = runTest {
        // GIVEN next payment is more than 3 days away
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(
                profile = profile().copy(nextPaymentDate = LocalDate.of(2026, 10, 27)),
            ),
            clock = MutableClock(LocalDate.of(2026, 10, 20)),
        )

        // WHEN / THEN
        useCase.statusFlow().test {
            val status = awaitItem() as AccountStatus
            assertEquals(AccountStatus.State.ALL_GOOD, status.state)
            assertEquals(7L, status.daysLeft)
            awaitComplete()
        }
    }

    @Test
    fun `active profile within due window is payment due`() = runTest {
        // GIVEN next payment is in 3 days
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(
                profile = profile().copy(nextPaymentDate = LocalDate.of(2026, 10, 27)),
            ),
            clock = MutableClock(LocalDate.of(2026, 10, 24)),
        )

        // WHEN / THEN
        useCase.statusFlow().test {
            val status = awaitItem() as AccountStatus
            assertEquals(AccountStatus.State.PAYMENT_DUE, status.state)
            assertEquals(3L, status.daysLeft)
            awaitComplete()
        }
    }

    @Test
    fun `active profile past due date is payment due`() = runTest {
        // GIVEN next payment date has passed
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(
                profile = profile().copy(nextPaymentDate = LocalDate.of(2026, 10, 27)),
            ),
            clock = MutableClock(LocalDate.of(2026, 10, 29)),
        )

        // WHEN / THEN
        useCase.statusFlow().test {
            val status = awaitItem() as AccountStatus
            assertEquals(AccountStatus.State.PAYMENT_DUE, status.state)
            assertEquals(-2L, status.daysLeft)
            awaitComplete()
        }
    }

    @Test
    fun `inactive profile is suspended regardless of due date`() = runTest {
        // GIVEN
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(
                profile = profile().copy(
                    isActive = false,
                    nextPaymentDate = LocalDate.of(2027, 12, 31),
                ),
            ),
            clock = MutableClock(LocalDate.of(2026, 10, 20)),
        )

        // WHEN / THEN
        useCase.statusFlow().test {
            val status = awaitItem() as AccountStatus
            assertEquals(AccountStatus.State.SUSPENDED, status.state)
            awaitComplete()
        }
    }

    @Test
    fun `inactive profile without next payment date is suspended`() = runTest {
        // GIVEN
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(
                profile = profile().copy(
                    isActive = false,
                    nextPaymentDate = null,
                ),
            ),
            clock = MutableClock(LocalDate.of(2026, 10, 20)),
        )

        // WHEN / THEN
        useCase.statusFlow().test {
            val status = awaitItem() as AccountStatus
            assertEquals(AccountStatus.State.SUSPENDED, status.state)
            assertNull(status.daysLeft)
            awaitComplete()
        }
    }

    @Test
    fun `no cached profile emits null`() = runTest {
        // GIVEN
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(profile = null),
            clock = MutableClock(LocalDate.of(2026, 10, 20)),
        )

        // WHEN / THEN
        useCase.statusFlow().test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
