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
import com.wasylews.mykompot.domain.MutableClock
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.FakePortalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : BaseViewModelTest() {
    @Test
    fun `get current state`() = runTest {
        // GIVEN
        val clock = MutableClock(LocalDate.of(2026, 10, 20))
        val useCase = GetStatusUseCase(FakePortalRepository(), clock)
        val vm = HomeViewModel(useCase)

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(R.string.all_good, state.status)
        assertEquals(R.string.all_good_message, state.message)
        assertEquals(LocalDate.of(2026, 10, 27), state.nextChargeDate)
        assertEquals(7L, state.daysUntilPayment)
        assertEquals("Standart 100", state.tariffName)
        assertEquals("300 ₴", state.tariffPrice)
    }
}
