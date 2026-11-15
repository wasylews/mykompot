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

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.ObserveStatus
import com.wasylews.mykompot.domain.ObserveTariffPlans
import com.wasylews.mykompot.domain.RefreshTariffPlans
import com.wasylews.mykompot.domain.model.TariffPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class TariffPlansUiState(
    val plans: List<TariffPlan> = emptyList(),
    val isVisible: Boolean = false,
    val selectedPlanIndex: Int? = null,
    val isLoading: Boolean = false,
    @StringRes val errorId: Int? = null,
)

@KoinViewModel
class TariffPlansViewModel(
    observeStatus: ObserveStatus,
    observeTariffPlans: ObserveTariffPlans,
    private val refreshTariffPlans: RefreshTariffPlans,
) : ViewModel() {
    private val isVisible = MutableStateFlow(false)
    private val isLoading = MutableStateFlow(false)
    private val loadErrorId = MutableStateFlow<Int?>(null)

    private val latestPlans = observeTariffPlans()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    init {
        refresh()
    }

    fun retry() {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            loadErrorId.value = null
            try {
                refreshTariffPlans()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                if (latestPlans.value.isNullOrEmpty()) {
                    loadErrorId.value = R.string.tariffs_load_error
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    val uiState: StateFlow<TariffPlansUiState> = combine(
        latestPlans,
        observeStatus.statusFlow().catch { error ->
            if (error is CancellationException) throw error
            emit(null)
        },
        isVisible,
        isLoading,
        loadErrorId,
    ) { plans, status, isVisible, isLoading, loadErrorId ->
        val list = plans.orEmpty()
        val selectedPlanIndex = status?.profile?.tariffPlan?.id?.let { id ->
            list.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        }
        TariffPlansUiState(
            plans = list,
            isVisible = isVisible,
            selectedPlanIndex = selectedPlanIndex,
            isLoading = isLoading,
            errorId = loadErrorId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TariffPlansUiState(),
    )

    fun open() {
        isVisible.value = true
    }

    fun dismiss() {
        isVisible.value = false
    }
}
