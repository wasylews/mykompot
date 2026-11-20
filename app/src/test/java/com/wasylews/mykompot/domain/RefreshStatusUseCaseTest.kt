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

import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.presentation.FakePortalRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RefreshStatusUseCaseTest {
    private val profile = Profile(
        isActive = true,
        nextPaymentDate = LocalDate.parse("2026-10-04"),
        tariffPlan = TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
    )

    @Test
    fun `refreshes status and schedules keep alive and reminders when updated`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Updated(profile), profile = profile)
        val scheduler = FakeSessionKeepAliveScheduler()
        val paymentScheduler = FakePaymentDueReminderScheduler()
        val useCase = RefreshStatusUseCase(repository, scheduler, paymentScheduler)

        val result = useCase()

        assertEquals(RefreshResult.Updated(profile), result)
        assertEquals(1, repository.refreshCalls)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
        assertEquals(1, paymentScheduler.scheduleCalls)
        assertEquals(profile, paymentScheduler.lastScheduledProfile)
        assertEquals(0, paymentScheduler.cancelCalls)
    }

    @Test
    fun `cancels keep alive and reminders when session expires`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Expired)
        val scheduler = FakeSessionKeepAliveScheduler()
        val paymentScheduler = FakePaymentDueReminderScheduler()
        val useCase = RefreshStatusUseCase(repository, scheduler, paymentScheduler)

        val result = useCase()

        assertEquals(RefreshResult.Expired, result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(0, paymentScheduler.scheduleCalls)
        assertEquals(1, paymentScheduler.cancelCalls)
    }

    @Test
    fun `does not change keep alive or reminders when refresh fails`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Failed(PortalFailure.Network))
        val scheduler = FakeSessionKeepAliveScheduler()
        val paymentScheduler = FakePaymentDueReminderScheduler()
        val useCase = RefreshStatusUseCase(repository, scheduler, paymentScheduler)

        val result = useCase()

        assertEquals(RefreshResult.Failed(PortalFailure.Network), result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
        assertEquals(0, paymentScheduler.scheduleCalls)
        assertEquals(0, paymentScheduler.cancelCalls)
    }
}
