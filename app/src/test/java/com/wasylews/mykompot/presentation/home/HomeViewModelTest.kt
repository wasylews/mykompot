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

import app.cash.turbine.test
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.domain.model.RefreshResult
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
    private val allGoodStatus = AccountStatus(
        state = AccountStatus.State.ALL_GOOD,
        profile = profile(),
        daysLeft = 7,
    )

    private val allGoodContent = HomeContent.Content(
        account = allGoodStatus,
        status = R.string.status_all_good,
        message = R.string.status_all_good_message,
        nextChargeLabel = R.string.next_charge,
    )

    private val paymentDueStatus = AccountStatus(
        state = AccountStatus.State.PAYMENT_DUE,
        profile = profile(),
        daysLeft = 2,
    )

    @Test
    fun `initial state is loading`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(allGoodStatus), FakeRefreshStatus(), FakeRefreshTariffPlans())

        // WHEN / THEN
        vm.uiState.test {
            assertEquals(HomeContent.Loading, awaitItem().content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `get current state`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(allGoodStatus), FakeRefreshStatus(), FakeRefreshTariffPlans())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(
            HomeUiState(content = allGoodContent, isRefreshing = false),
            state,
        )
    }

    @Test
    fun `get payment due state`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(paymentDueStatus), FakeRefreshStatus(), FakeRefreshTariffPlans())

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
                isRefreshing = false,
            ),
            state,
        )
    }

    @Test
    fun `shows loading while refreshing then unknown when no cached profile`() = runTest {
        // GIVEN no cached profile and a refresh that is still in progress
        val refreshStatus = FakeRefreshStatus().apply { pause() }
        val vm = HomeViewModel(FakeObserveStatusUseCase(status = null), refreshStatus, FakeRefreshTariffPlans())

        vm.uiState.test {
            // WHEN the screen opens
            assertEquals(HomeContent.Loading, awaitItem().content)

            // THEN I see loading while refreshing
            advanceUntilIdle()
            assertEquals(
                HomeUiState(content = HomeContent.Loading, isRefreshing = true),
                expectMostRecentItem(),
            )

            // WHEN the refresh completes with no cached profile
            refreshStatus.resume()
            advanceUntilIdle()

            // THEN I see the unknown status state
            assertEquals(
                HomeUiState(content = HomeContent.Unknown, isRefreshing = false),
                awaitItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `profile load error maps to unknown`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(error = IOException()), FakeRefreshStatus(), FakeRefreshTariffPlans())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertEquals(HomeUiState(content = HomeContent.Unknown, isRefreshing = false), state)
    }

    @Test
    fun `cancellation is not mapped to unknown`() = runTest {
        // GIVEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(error = CancellationException()), FakeRefreshStatus(), FakeRefreshTariffPlans())

        // WHEN
        advanceUntilIdle()
        val state = vm.uiState.value

        // THEN
        assertNotEquals(HomeContent.Unknown, state.content)
    }

    @Test
    fun `refreshes status on init`() = runTest {
        // GIVEN
        val refreshStatus = FakeRefreshStatus()

        // WHEN
        HomeViewModel(FakeObserveStatusUseCase(allGoodStatus), refreshStatus, FakeRefreshTariffPlans())
        advanceUntilIdle()

        // THEN
        assertEquals(1, refreshStatus.calls)
    }

    @Test
    fun `keeps content and shows server error when cached profile refresh fails`() = runTest {
        // GIVEN a cached profile and a refresh that fails with a server error
        val refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Server)

        // WHEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(allGoodStatus), refreshStatus, FakeRefreshTariffPlans())
        advanceUntilIdle()

        // THEN
        assertEquals(
            HomeUiState(
                content = allGoodContent,
                isRefreshing = false,
                errorId = R.string.error_server,
            ),
            vm.uiState.value,
        )
    }

    @Test
    fun `keeps content and shows connection error when cached profile refresh fails`() = runTest {
        // GIVEN a cached profile and a refresh that fails with a network error
        val refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Network)

        // WHEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(allGoodStatus), refreshStatus, FakeRefreshTariffPlans())
        advanceUntilIdle()

        // THEN
        assertEquals(
            HomeUiState(
                content = allGoodContent,
                isRefreshing = false,
                errorId = R.string.error_no_connection,
            ),
            vm.uiState.value,
        )
    }

    @Test
    fun `shows unknown without error when no cached profile refresh fails`() = runTest {
        // GIVEN no cached profile and a refresh that fails
        val refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Server)

        // WHEN
        val vm = HomeViewModel(FakeObserveStatusUseCase(status = null), refreshStatus, FakeRefreshTariffPlans())
        advanceUntilIdle()

        // THEN
        assertEquals(
            HomeUiState(content = HomeContent.Unknown, isRefreshing = false),
            vm.uiState.value,
        )
    }

    @Test
    fun `clearError clears refresh error`() = runTest {
        // GIVEN a cached profile and a failed refresh
        val vm = HomeViewModel(
            FakeObserveStatusUseCase(allGoodStatus),
            FakeRefreshStatus(result = RefreshResult.Failed.Server),
            FakeRefreshTariffPlans(),
        )
        advanceUntilIdle()

        // WHEN
        vm.clearError()
        advanceUntilIdle()

        // THEN
        assertEquals(null, vm.uiState.value.errorId)
    }


    @Test
    fun `refreshes tariff plans catalog on init`() = runTest {
        // GIVEN
        val refreshTariffPlans = FakeRefreshTariffPlans()

        // WHEN
        HomeViewModel(
            FakeObserveStatusUseCase(allGoodStatus),
            FakeRefreshStatus(),
            refreshTariffPlans,
        )
        advanceUntilIdle()

        // THEN
        assertEquals(1, refreshTariffPlans.calls)
    }

    @Test
    fun `suspended account without next payment date hides next charge`() = runTest {
        // GIVEN
        val status = AccountStatus(
            state = AccountStatus.State.SUSPENDED,
            profile = profile().copy(
                isActive = false,
                nextPaymentDate = null,
            ),
            daysLeft = null,
        )
        val vm = HomeViewModel(FakeObserveStatusUseCase(status), FakeRefreshStatus(), FakeRefreshTariffPlans())

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
