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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        profile(),
        daysLeft = 2,
    )

    @Test
    fun `initial state is loading`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(allGoodStatus), FakeRefreshStatus())

        // WHEN
        val state = vm.uiState.value

        // THEN
        assertEquals(HomeUiState(), state)
    }

    @Test
    fun `get current state`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(allGoodStatus), FakeRefreshStatus())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(
            HomeUiState(
                content = HomeContent.Content(
                    account = allGoodStatus,
                    status = R.string.status_all_good,
                    message = R.string.status_all_good_message,
                    nextChargeLabel = R.string.next_charge,
                ),
            ),
            state,
        )
    }

    @Test
    fun `get payment due state`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(paymentDueStatus), FakeRefreshStatus())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(
            HomeUiState(
                content = HomeContent.Content(
                    account = paymentDueStatus,
                    status = R.string.status_payment_due,
                    message = R.string.status_payment_due_message,
                    nextChargeLabel = R.string.due,
                ),
            ),
            state,
        )
    }

    @Test
    fun `no cached profile maps to unavailable`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(AccountStatus.NoProfile), FakeRefreshStatus())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(HomeUiState(content = HomeContent.Unavailable), state)
    }

    @Test
    fun `profile load error maps to unavailable`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(error = IOException()), FakeRefreshStatus())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(HomeUiState(content = HomeContent.Unavailable), state)
    }

    @Test
    fun `cancellation is not mapped to unavailable`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(error = CancellationException()), FakeRefreshStatus())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertNotEquals(HomeContent.Unavailable, state.content)
    }

    @Test
    fun `refreshes status on init`() = runTest {
        // GIVEN
        val refreshStatus = FakeRefreshStatus()

        // WHEN
        HomeViewModel(FakeObserveStatusUseCase(allGoodStatus), refreshStatus)
        advanceUntilIdle()

        // THEN
        assertEquals(1, refreshStatus.calls)
    }

    @Test
    fun `suspended account without next payment date hides next charge`() = runTest {
        // GIVEN
        val status = AccountStatus.Known(
            state = AccountStatus.State.SUSPENDED,
            profile = profile().copy(
                isActive = false,
                nextPaymentDate = null,
            ),
            daysLeft = null,
        )
        val vm = HomeViewModel(FakeObserveStatusUseCase(status), FakeRefreshStatus())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(
            HomeUiState(
                content = HomeContent.Content(
                    account = status,
                    status = R.string.status_suspended,
                    message = R.string.status_suspended_message,
                    nextChargeLabel = null,
                ),
            ),
            state,
        )
    }
}
