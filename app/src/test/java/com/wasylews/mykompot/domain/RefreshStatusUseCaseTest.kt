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

import com.wasylews.mykompot.ProfileFixtures
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.presentation.FakePortalRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RefreshStatusUseCaseTest {
    private val profile = ProfileFixtures.profile(nextPaymentDate = LocalDate.parse("2026-10-04"))

    @Test
    fun `returns updated status`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Updated(profile), profile = profile)
        val useCase = RefreshStatusUseCase(repository)

        val result = useCase()

        assertEquals(RefreshResult.Updated(profile), result)
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `returns expired status`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Expired)
        val useCase = RefreshStatusUseCase(repository)

        val result = useCase()

        assertEquals(RefreshResult.Expired, result)
    }

    @Test
    fun `returns failure`() = runTest {
        val repository = FakePortalRepository(refreshResult = RefreshResult.Failed(PortalFailure.Network))
        val useCase = RefreshStatusUseCase(repository)

        val result = useCase()

        assertEquals(RefreshResult.Failed(PortalFailure.Network), result)
    }
}
