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
import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.ObserveStatus
import com.wasylews.mykompot.domain.RefreshStatus
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.FakeObserveStatusUseCase
import com.wasylews.mykompot.presentation.FakeRefreshStatus
import com.wasylews.mykompot.presentation.main.FakeObserveSession
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        observeSession: ObserveSession = FakeObserveSession(SessionState.LoggedIn),
    ) = HomeViewModel(observeStatus, refreshStatus, observeSession)

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
    fun `cached profile is shown without pull to refresh indicator during background refresh`() = runTest {
        // GIVEN a cached profile and a background refresh that is still in progress
        val refreshStatus = FakeRefreshStatus().apply { pause() }
        val vm = createViewModel(refreshStatus = refreshStatus)

        vm.uiState.test {
            // WHEN the screen opens before the cache has been read
            assertEquals(HomeContent.Loading, awaitItem().content)

            // THEN the cached content appears without a pull-to-refresh indicator
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(allGoodContent, state.content)
            assertFalse(state.isRefreshing)

            refreshStatus.resume()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
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
    fun `shows loading while refreshing then unknown with server error when no cached profile`() = runTest {
        // GIVEN no cached profile and a refresh that is still in progress
        val refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Server).apply {
            pause()
        }
        val vm = createViewModel(
            observeStatus = FakeObserveStatusUseCase(status = null),
            refreshStatus = refreshStatus,
        )

        vm.uiState.test {
            // WHEN the screen opens before the cached status has been read
            assertEquals(HomeContent.Loading, awaitItem().content)

            // THEN I still see loading once there is no cached status and a refresh is running
            advanceUntilIdle()
            assertEquals(
                HomeUiState(content = HomeContent.Loading, isRefreshing = false),
                vm.uiState.value,
            )

            // WHEN the refresh completes with no cached profile
            refreshStatus.resume()
            advanceUntilIdle()

            // THEN I see the unknown status and the server error
            assertEquals(
                HomeUiState(
                    content = HomeContent.Unknown,
                    isRefreshing = false,
                    errorId = R.string.error_server,
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
    fun `pull to refresh shows indicator while refreshing`() = runTest {
        // GIVEN a cached profile and an initial background refresh completed
        val refreshStatus = FakeRefreshStatus().apply { pause() }
        val vm = createViewModel(refreshStatus = refreshStatus)

        vm.uiState.test {
            assertEquals(HomeContent.Loading, awaitItem().content)
            advanceUntilIdle()
            assertEquals(
                HomeUiState(content = allGoodContent, isRefreshing = false),
                expectMostRecentItem(),
            )

            refreshStatus.resume()
            advanceUntilIdle()
            assertEquals(
                HomeUiState(content = allGoodContent, isRefreshing = false),
                vm.uiState.value,
            )

            // WHEN I pull to refresh
            refreshStatus.pause()
            vm.refresh(userInitiated = true)
            advanceUntilIdle()

            // THEN I see the pull-to-refresh indicator while keeping content
            assertEquals(
                HomeUiState(
                    content = allGoodContent,
                    isRefreshing = true,
                ),
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
    fun `background refresh failure keeps cached content and shows error`() = runTest {
        // GIVEN a cached profile and a successful initial refresh
        val refreshStatus = FakeRefreshStatus()
        val vm = createViewModel(refreshStatus = refreshStatus)
        advanceUntilIdle()

        // WHEN a background refresh fails
        refreshStatus.result = RefreshResult.Failed.Network
        vm.refresh()
        advanceUntilIdle()

        // THEN cached content stays and the connection error is shown without a PTR indicator
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

    @Test
    fun `retry with no cached profile shows loading then content instead of stale error`() = runTest {
        // GIVEN a failed refresh with no cached profile
        val statusFlow = MutableStateFlow<AccountStatus?>(null)
        val refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Server)
        val vm = createViewModel(
            observeStatus = ObserveStatus { statusFlow },
            refreshStatus = refreshStatus,
        )
        advanceUntilIdle()
        assertEquals(
            HomeUiState(content = HomeContent.Unknown, errorId = R.string.error_server),
            vm.uiState.value,
        )

        // WHEN a retry starts
        refreshStatus.result = RefreshResult.Updated(profile())
        refreshStatus.pause()
        vm.refresh()
        advanceUntilIdle()

        // THEN the skeleton replaces the stale error state
        assertEquals(
            HomeUiState(content = HomeContent.Loading),
            vm.uiState.value,
        )

        // AND the cached status emitted by the refresh replaces the skeleton
        refreshStatus.resume()
        statusFlow.value = allGoodStatus
        advanceUntilIdle()
        assertEquals(HomeUiState(content = allGoodContent), vm.uiState.value)
    }

    @Test
    fun `user refresh during running refresh only shows indicator`() = runTest {
        // GIVEN a background refresh that is still in progress
        val refreshStatus = FakeRefreshStatus().apply { pause() }
        val vm = createViewModel(refreshStatus = refreshStatus)
        advanceUntilIdle()
        assertEquals(1, refreshStatus.calls)
        assertFalse(vm.uiState.value.isRefreshing)

        // WHEN the user pulls to refresh while it is still running
        vm.refresh(userInitiated = true)
        advanceUntilIdle()

        // THEN the indicator shows without starting a second refresh
        assertTrue(vm.uiState.value.isRefreshing)
        assertEquals(1, refreshStatus.calls)

        // AND the indicator goes away when the running refresh finishes
        refreshStatus.resume()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isRefreshing)
        assertEquals(1, refreshStatus.calls)
    }

    @Test
    fun `user refresh during failing refresh keeps running refresh error`() = runTest {
        // GIVEN a background refresh that is still in progress and will fail
        val refreshStatus = FakeRefreshStatus(result = RefreshResult.Failed.Server).apply { pause() }
        val vm = createViewModel(refreshStatus = refreshStatus)
        advanceUntilIdle()
        assertEquals(1, refreshStatus.calls)
        assertFalse(vm.uiState.value.isRefreshing)

        // WHEN the user pulls to refresh while it is still running
        vm.refresh(userInitiated = true)
        advanceUntilIdle()

        // THEN the user call does not clear the running refresh's eventual error
        assertTrue(vm.uiState.value.isRefreshing)
        assertEquals(1, refreshStatus.calls)

        refreshStatus.resume()
        advanceUntilIdle()
        assertEquals(
            HomeUiState(content = allGoodContent, errorId = R.string.error_server),
            vm.uiState.value,
        )
        assertFalse(vm.uiState.value.isRefreshing)
        assertEquals(1, refreshStatus.calls)
    }
}
