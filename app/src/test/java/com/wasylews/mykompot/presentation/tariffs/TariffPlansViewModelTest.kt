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

import com.wasylews.mykompot.domain.ObserveStatus
import com.wasylews.mykompot.domain.ObserveTariffPlans
import com.wasylews.mykompot.domain.RefreshTariffPlans
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.FakeObserveStatusUseCase
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    ) = TariffPlansViewModel(observeStatus, observeTariffPlans, refreshTariffPlans)

    @Test
    fun `refreshes tariff plans on init`() = runTest {
        // GIVEN
        val refreshTariffPlans = FakeRefreshTariffPlans()

        // WHEN
        createViewModel(refreshTariffPlans = refreshTariffPlans)
        advanceUntilIdle()

        // THEN
        assertEquals(1, refreshTariffPlans.calls)
    }

    @Test
    fun `exposes tariff plans and selected plan`() = runTest {
        // GIVEN
        val vm = createViewModel()

        // WHEN
        advanceUntilIdle()

        // THEN
        assertEquals(
            TariffPlansUiState(plans = plans, isVisible = false, selectedPlanIndex = 1),
            vm.uiState.value,
        )
    }

    @Test
    fun `open shows tariff plans sheet`() = runTest {
        // GIVEN
        val vm = createViewModel()
        advanceUntilIdle()

        // WHEN
        vm.open()
        advanceUntilIdle()

        // THEN
        assertTrue(vm.uiState.value.isVisible)
    }

    @Test
    fun `dismiss hides tariff plans sheet`() = runTest {
        // GIVEN
        val vm = createViewModel()
        advanceUntilIdle()
        vm.open()
        advanceUntilIdle()

        // WHEN
        vm.dismiss()
        advanceUntilIdle()

        // THEN
        assertFalse(vm.uiState.value.isVisible)
    }
}
