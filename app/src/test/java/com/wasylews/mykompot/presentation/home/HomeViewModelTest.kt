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

package com.wasylews.mykompot.presentation.home

import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.GetStatusUseCase
import com.wasylews.mykompot.presentation.FakePortalRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Clock

class HomeViewModelTest {

    @Test
    fun `get current state`() {
        // GIVEN
        val useCase = GetStatusUseCase(FakePortalRepository(), Clock.System)
        val vm = HomeViewModel(useCase)

        // WHEN
        val state = vm.uiState.value

        // THEN
        assertEquals(R.string.all_good, state.status)
        assertEquals(R.string.everything_is_working, state.reason)
        assertEquals(R.string.account_is_active, state.note)
        assertEquals("1 Nov", state.nextChargeDate)
        assertEquals("in 20 days", state.countdown)
        assertEquals("Standart 100", state.tariffName)
        assertEquals("300 ₴", state.tariffPrice)
    }
}
