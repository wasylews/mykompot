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

package com.wasylews.mykompot.presentation.tariffs

import app.cash.turbine.test
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.ObserveStatus
import com.wasylews.mykompot.domain.ObserveTariffPlans
import com.wasylews.mykompot.domain.RefreshTariffPlans
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlansActionResult
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.FakeObserveStatusUseCase
import com.wasylews.mykompot.presentation.main.FakeObserveSession
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TariffPlansViewModelTest : BaseViewModelTest() {
    private val plans = listOf(
        TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
        TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
        TariffPlan(id = "350GRN_300mb", name = "LUX 300", speed = 300, price = 350),
    )

    private val status = AccountStatus(
        state = AccountStatus.State.ALL_GOOD,
        profile = profile().copy(tariffPlan = plans[1]),
        daysLeft = 7,
    )

    private fun createViewModel(
        observeStatus: ObserveStatus = FakeObserveStatusUseCase(status),
        observeTariffPlans: ObserveTariffPlans = FakeObserveTariffPlans(plans),
        refreshTariffPlans: RefreshTariffPlans = FakeRefreshTariffPlans(),
        observeSession: ObserveSession = FakeObserveSession(SessionState.LoggedIn),
    ) = TariffPlansViewModel(observeStatus, observeTariffPlans, refreshTariffPlans, observeSession)

    @Test
    fun `refreshes tariff plans on init and exposes plans with selection`() = runTest {
        // GIVEN
        val refreshTariffPlans = FakeRefreshTariffPlans()

        // WHEN
        val vm = createViewModel(refreshTariffPlans = refreshTariffPlans)
        advanceUntilIdle()

        // THEN
        assertEquals(1, refreshTariffPlans.calls)
        assertEquals(
            TariffPlansUiState(plans = plans, isVisible = false, selectedPlanIndex = 1),
            vm.uiState.value,
        )
    }

    @Test
    fun `open and dismiss toggle sheet visibility`() = runTest {
        // GIVEN
        val vm = createViewModel()
        advanceUntilIdle()

        // WHEN / THEN
        vm.open()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isVisible)

        vm.dismiss()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isVisible)
    }

    @Test
    fun `shows loading while refreshing with no cached plans`() = runTest {
        // GIVEN no cached plans and a refresh that is still in progress
        val refreshTariffPlans = FakeRefreshTariffPlans().apply { pause() }
        val vm = createViewModel(
            observeTariffPlans = FakeObserveTariffPlans(plans = null),
            refreshTariffPlans = refreshTariffPlans,
        )

        vm.uiState.test {
            awaitItem()

            // WHEN the screen opens
            advanceUntilIdle()

            // THEN loading is shown without plans or error
            assertEquals(
                TariffPlansUiState(
                    plans = emptyList(),
                    isVisible = false,
                    selectedPlanIndex = null,
                    isLoading = true,
                    errorId = null,
                ),
                expectMostRecentItem(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shows error when refresh fails without cached plans`() = runTest {
        // GIVEN no cached plans and refresh fails
        val refreshTariffPlans = FakeRefreshTariffPlans().apply {
            result = TariffPlansActionResult.Failed.Network
        }
        val vm = createViewModel(
            observeTariffPlans = FakeObserveTariffPlans(plans = null),
            refreshTariffPlans = refreshTariffPlans,
        )

        // WHEN
        advanceUntilIdle()

        // THEN
        assertEquals(
            TariffPlansUiState(
                plans = emptyList(),
                isVisible = false,
                selectedPlanIndex = null,
                isLoading = false,
                errorId = R.string.tariffs_load_error,
            ),
            vm.uiState.value,
        )
    }

    @Test
    fun `keeps cached plans and hides error when refresh fails`() = runTest {
        // GIVEN cached plans and refresh fails
        val refreshTariffPlans = FakeRefreshTariffPlans().apply {
            result = TariffPlansActionResult.Failed.Network
        }
        val vm = createViewModel(
            observeTariffPlans = FakeObserveTariffPlans(plans),
            refreshTariffPlans = refreshTariffPlans,
        )

        // WHEN
        advanceUntilIdle()

        // THEN
        assertEquals(
            TariffPlansUiState(
                plans = plans,
                isVisible = false,
                selectedPlanIndex = 1,
                isLoading = false,
                errorId = null,
            ),
            vm.uiState.value,
        )
    }

    @Test
    fun `treats not modified refresh as success`() = runTest {
        // GIVEN no cached plans and the server reports they are unchanged
        val refreshTariffPlans = FakeRefreshTariffPlans().apply {
            result = TariffPlansActionResult.NotModified
        }
        val vm = createViewModel(
            observeTariffPlans = FakeObserveTariffPlans(plans = null),
            refreshTariffPlans = refreshTariffPlans,
        )

        // WHEN
        advanceUntilIdle()

        // THEN no error is shown
        assertNull(vm.uiState.value.errorId)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `retry loads plans after failure`() = runTest {
        // GIVEN refresh fails initially with no cached plans
        val observeTariffPlans = FakeObserveTariffPlans(plans = null)
        val refreshTariffPlans = FakeRefreshTariffPlans().apply {
            result = TariffPlansActionResult.Failed.Network
        }
        val vm = createViewModel(
            observeTariffPlans = observeTariffPlans,
            refreshTariffPlans = refreshTariffPlans,
        )
        advanceUntilIdle()
        assertEquals(R.string.tariffs_load_error, vm.uiState.value.errorId)

        // WHEN refresh succeeds on retry and plans become available
        refreshTariffPlans.result = TariffPlansActionResult.Success
        refreshTariffPlans.pause()
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorId)

        observeTariffPlans.setPlans(plans)
        refreshTariffPlans.resume()
        advanceUntilIdle()

        // THEN
        assertEquals(2, refreshTariffPlans.calls)
        assertEquals(
            TariffPlansUiState(
                plans = plans,
                isVisible = false,
                selectedPlanIndex = 1,
                isLoading = false,
                errorId = null,
            ),
            vm.uiState.value,
        )
    }

    @Test
    fun `retry shows loading then error when refresh fails again`() = runTest {
        // GIVEN refresh fails with no cached plans
        val refreshTariffPlans = FakeRefreshTariffPlans().apply {
            result = TariffPlansActionResult.Failed.Network
        }
        val vm = createViewModel(
            observeTariffPlans = FakeObserveTariffPlans(plans = null),
            refreshTariffPlans = refreshTariffPlans,
        )
        advanceUntilIdle()

        vm.uiState.test {
            assertEquals(R.string.tariffs_load_error, expectMostRecentItem().errorId)

            // WHEN retry starts
            refreshTariffPlans.pause()
            vm.retry()
            advanceUntilIdle()

            // THEN loading is shown and error cleared
            val loading = expectMostRecentItem()
            assertTrue(loading.isLoading)
            assertNull(loading.errorId)

            // WHEN retry completes with failure
            refreshTariffPlans.resume()
            advanceUntilIdle()

            // THEN error is shown again
            val failed = awaitItem()
            assertFalse(failed.isLoading)
            assertEquals(R.string.tariffs_load_error, failed.errorId)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
