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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.GetStatus
import com.wasylews.mykompot.domain.model.AccountStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
    private val getStatusUseCase: GetStatus,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            when (val status = getStatusUseCase()) {
                is AccountStatus.Known -> _uiState.value = status.toHomeUiState()
                AccountStatus.NoProfile -> Unit
            }
        }
    }

    private fun AccountStatus.Known.toHomeUiState(): HomeUiState {
        val status = when (state) {
            AccountStatus.State.ALL_GOOD -> R.string.status_all_good
            AccountStatus.State.PAYMENT_DUE -> R.string.status_payment_due
            AccountStatus.State.UNKNOWN -> R.string.status_unknown
        }

        val message = when (state) {
            AccountStatus.State.ALL_GOOD -> R.string.status_all_good_message
            AccountStatus.State.PAYMENT_DUE -> R.string.status_payment_due_message
            AccountStatus.State.UNKNOWN -> R.string.status_unknown_message
        }

        val nextChargeLabel = when (state) {
            AccountStatus.State.ALL_GOOD -> R.string.next_charge
            AccountStatus.State.PAYMENT_DUE -> R.string.due
            AccountStatus.State.UNKNOWN -> R.string.next_charge
        }

        return HomeUiState(
            accountState = state,
            status = status,
            message = message,
            nextChargeLabel = nextChargeLabel,
            nextChargeDate = profile.nextPaymentDate,
            daysUntilPayment = daysLeft,
            tariffName = profile.tariffPlan.name,
            tariffPrice = profile.tariffPlan.price,
        )
    }
}
