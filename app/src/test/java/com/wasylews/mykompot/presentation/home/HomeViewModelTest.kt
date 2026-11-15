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
import com.wasylews.mykompot.domain.ObserveStatus
import com.wasylews.mykompot.domain.RefreshStatus
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.FakeObserveStatusUseCase
import com.wasylews.mykompot.presentation.FakeRefreshStatus
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun createViewModel(
        observeStatus: ObserveStatus = FakeObserveStatusUseCase(allGoodStatus),
        refreshStatus: RefreshStatus = FakeRefreshStatus(),
    ) = HomeViewModel(observeStatus, refreshStatus)

    @Test
    fun `shows cached content after refresh on init`() = runTest {
        // GIVEN
        val refreshStatus = FakeRefreshStatus()

        // WHEN
        val vm = createViewModel(refreshStatus = refreshStatus)
        advanceUntilIdle()

        // THEN
        assertEquals(1, refreshStatus.calls)
        assertEquals(
            HomeUiState(content = allGoodContent, isRefreshing = false),
            vm.uiState.value,
        )
    }

    @Test
    fun `maps payment due status to content`() = runTest {
        // GIVEN
        val vm = createViewModel(observeStatus = FakeObserveStatusUseCase(paymentDueStatus))

        // WHEN
        advanceUntilIdle()

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
            vm.uiState.value,
        )
    }

    @Test
    fun `maps suspended status without next payment date to content without next charge`() = runTest {
        // GIVEN
        val status = AccountStatus(
            state = AccountStatus.State.SUSPENDED,
            profile = profile().copy(
                isActive = false,
                nextPaymentDate = null,
            ),
            daysLeft = null,
        )
        val vm = createViewModel(observeStatus = FakeObserveStatusUseCase(status))

        // WHEN
        advanceUntilIdle()

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
            vm.uiState.value,
        )
    }

    @Test
    fun `shows loading while refreshing then unknown without error when no cached profile`() = runTest {
        // GIVEN no cached profile and a refresh that is still in progress
        val refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Server).apply {
            pause()
        }
        val vm = createViewModel(
            observeStatus = FakeObserveStatusUseCase(status = null),
            refreshStatus = refreshStatus,
        )

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

            // THEN I see the unknown status without a snackbar error
            assertEquals(
                HomeUiState(
                    content = HomeContent.Unknown,
                    isRefreshing = false,
                    errorId = null,
                ),
                awaitItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps content and shows server error when cached profile refresh fails`() = runTest {
        // GIVEN a cached profile and a refresh that fails with a server error
        val refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Server)

        // WHEN
        val vm = createViewModel(refreshStatus = refreshStatus)
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
        val vm = createViewModel(refreshStatus = refreshStatus)
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
    fun `clearError clears refresh error`() = runTest {
        // GIVEN a cached profile and a failed refresh
        val vm = createViewModel(
            refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Server),
        )
        advanceUntilIdle()

        // WHEN
        vm.clearError()
        advanceUntilIdle()

        // THEN
        assertEquals(
            HomeUiState(
                content = allGoodContent,
                isRefreshing = false,
                errorId = null,
            ),
            vm.uiState.value,
        )
    }

    @Test
    fun `refresh can be triggered manually`() = runTest {
        // GIVEN a cached profile and an initial refresh completed
        val refreshStatus = FakeRefreshStatus().apply { pause() }
        val vm = createViewModel(refreshStatus = refreshStatus)

        vm.uiState.test {
            assertEquals(HomeContent.Loading, awaitItem().content)
            advanceUntilIdle()
            assertEquals(
                HomeUiState(content = allGoodContent, isRefreshing = true),
                expectMostRecentItem(),
            )

            refreshStatus.resume()
            advanceUntilIdle()
            assertEquals(
                HomeUiState(content = allGoodContent, isRefreshing = false),
                awaitItem(),
            )

            // WHEN I refresh again
            refreshStatus.pause()
            vm.refresh()
            advanceUntilIdle()

            // THEN I see the refreshing state while keeping content
            assertEquals(
                HomeUiState(content = allGoodContent, isRefreshing = true),
                awaitItem(),
            )
            assertEquals(2, refreshStatus.calls)

            refreshStatus.resume()
            advanceUntilIdle()
            assertEquals(
                HomeUiState(content = allGoodContent, isRefreshing = false),
                awaitItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `manual refresh failure keeps cached content and shows error`() = runTest {
        // GIVEN a cached profile and a successful initial refresh
        val refreshStatus = FakeRefreshStatus()
        val vm = createViewModel(refreshStatus = refreshStatus)
        advanceUntilIdle()

        // WHEN a manual refresh fails
        refreshStatus.result = RefreshResult.Failed.Network
        vm.refresh()
        advanceUntilIdle()

        // THEN cached content stays and the connection error is shown
        assertEquals(
            HomeUiState(
                content = allGoodContent,
                isRefreshing = false,
                errorId = R.string.error_no_connection,
            ),
            vm.uiState.value,
        )
        assertEquals(2, refreshStatus.calls)
    }
}
