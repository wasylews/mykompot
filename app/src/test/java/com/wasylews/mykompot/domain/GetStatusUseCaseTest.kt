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

import com.wasylews.mykompot.presentation.FakePortalRepository
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

class GetStatusUseCaseTest {

    @Test
    fun `load user profile`() = runTest {
        // GIVEN
        val useCase = GetStatusUseCase(
            repository = FakePortalRepository(),
            clock = Clock.System
        )

        // WHEN
        val profile = useCase()

        // THEN
        assertTrue(profile.isActive)
    }

    @Test
    fun `load user profile 3 days before due date`() = runTest {
        // GIVEN
        val clock = MutableClock(Clock.System.now())
        val useCase = GetStatusUseCase(
            repository = FakePortalRepository(profile = profile(isActive = false)),
            clock = clock
        )

        // WHEN
        val profile = useCase()

        // THEN
        assertFalse(profile.isActive)
    }
}
