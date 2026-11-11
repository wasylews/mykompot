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

import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.presentation.FakePortalRepository
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ObserveStatusUseCaseTest {
    @Test
    fun `load account status`() = runTest {
        // GIVEN
        val clock = MutableClock(LocalDate.of(2026, 10, 20))
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(),
            clock = clock,
        )

        // WHEN
        val result = useCase.statusFlow().first()

        // THEN
        val status = result as AccountStatus.Known
        assertEquals(AccountStatus.State.ALL_GOOD, status.state)
    }

    @Test
    fun `load user profile 3 days before due date`() = runTest {
        // GIVEN
        val clock = MutableClock(LocalDate.of(2026, 10, 29))
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(profile = profile()),
            clock = clock,
        )

        // WHEN
        val result = useCase.statusFlow().first()

        // THEN
        val status = result as AccountStatus.Known
        assertEquals(AccountStatus.State.PAYMENT_DUE, status.state)
        assertEquals(-2L, status.daysLeft)
    }

    @Test
    fun `inactive profile is suspended regardless of date`() = runTest {
        // GIVEN
        val clock = MutableClock(LocalDate.of(2026, 10, 20))
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(
                profile = profile().copy(
                    isActive = false,
                    nextPaymentDate = LocalDate.of(2027, 12, 31),
                ),
            ),
            clock = clock,
        )

        // WHEN
        val result = useCase.statusFlow().first()

        // THEN
        val status = result as AccountStatus.Known
        assertEquals(AccountStatus.State.SUSPENDED, status.state)
    }

    @Test
    fun `inactive profile without next payment date is suspended`() = runTest {
        // GIVEN
        val clock = MutableClock(LocalDate.of(2026, 10, 20))
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(
                profile = profile().copy(
                    isActive = false,
                    nextPaymentDate = null,
                ),
            ),
            clock = clock,
        )

        // WHEN
        val result = useCase.statusFlow().first()

        // THEN
        val status = result as AccountStatus.Known
        assertEquals(AccountStatus.State.SUSPENDED, status.state)
        assertEquals(null, status.daysLeft)
    }

    @Test
    fun `no cached profile`() = runTest {
        // GIVEN
        val useCase = ObserveStatusUseCase(
            repository = FakePortalRepository(profile = null),
            clock = MutableClock(LocalDate.of(2026, 10, 20)),
        )

        // WHEN
        val result = useCase.statusFlow().first()

        // THEN
        assertEquals(AccountStatus.NoProfile, result)
    }
}
