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

import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.presentation.FakePortalRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshStatusUseCaseTest {
    @Test
    fun `refreshes status and schedules keep alive when updated`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Updated)
        val scheduler = FakeSessionKeepAliveScheduler()
        val useCase = RefreshStatusUseCase(repository, scheduler)

        val result = useCase()

        assertEquals(RefreshResult.Updated, result)
        assertEquals(1, repository.refreshCalls)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }

    @Test
    fun `cancels keep alive when session expires`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Expired)
        val scheduler = FakeSessionKeepAliveScheduler()
        val useCase = RefreshStatusUseCase(repository, scheduler)

        val result = useCase()

        assertEquals(RefreshResult.Expired, result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(1, scheduler.cancelCalls)
    }

    @Test
    fun `does not change keep alive when refresh fails`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Failed.Network)
        val scheduler = FakeSessionKeepAliveScheduler()
        val useCase = RefreshStatusUseCase(repository, scheduler)

        val result = useCase()

        assertEquals(RefreshResult.Failed.Network, result)
        assertEquals(0, scheduler.scheduleCalls)
        assertEquals(0, scheduler.cancelCalls)
    }
}
