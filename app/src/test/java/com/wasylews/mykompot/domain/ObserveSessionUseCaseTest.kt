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
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.presentation.FakePortalRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveSessionUseCaseTest {

    @Test
    fun `no stored session returns LoggedOut`() = runTest {
        val useCase = ObserveSessionUseCase(FakePortalRepository())

        useCase.sessionStateFlow().test {
            assertEquals(SessionState.LoggedOut, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `stored session returns LoggedOut`() = runTest {
        val useCase = ObserveSessionUseCase(FakePortalRepository(session = "session-id"))

        useCase.sessionStateFlow().test {
            assertEquals(SessionState.LoggedIn, awaitItem())
            awaitComplete()
        }
    }
}
