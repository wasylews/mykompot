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
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : BaseViewModelTest() {
    private val allGoodStatus = AccountStatus.Known(
        AccountStatus.State.ALL_GOOD,
        profile(),
        daysLeft = 7,
    )

    private val paymentDueStatus = AccountStatus.Known(
        AccountStatus.State.PAYMENT_DUE,
        profile().copy(isActive = false),
        daysLeft = -2,
    )

    @Test
    fun `initial state is loading`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeGetStatusUseCase(allGoodStatus))

        // WHEN
        val state = vm.uiState.value

        // THEN
        assertEquals(HomeUiState.Loading, state)
    }

    @Test
    fun `get current state`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeGetStatusUseCase(allGoodStatus))

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(
            HomeUiState.Content(
                account = allGoodStatus,
                status = R.string.status_all_good,
                message = R.string.status_all_good_message,
                nextChargeLabel = R.string.next_charge,
            ),
            state,
        )
    }

    @Test
    fun `get payment due state`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeGetStatusUseCase(paymentDueStatus))

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(
            HomeUiState.Content(
                account = paymentDueStatus,
                status = R.string.status_payment_due,
                message = R.string.status_payment_due_message,
                nextChargeLabel = R.string.due,
            ),
            state,
        )
    }

    @Test
    fun `no cached profile maps to no account`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeGetStatusUseCase(AccountStatus.NoProfile))

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(HomeUiState.NoAccount, state)
    }

    @Test
    fun `profile load error maps to error`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeGetStatusUseCase(error = IOException()))

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(HomeUiState.Error, state)
    }
}
