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

import androidx.annotation.StringRes
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
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = loadState()
        }
    }

    private suspend fun loadState(): HomeUiState = runCatching {
        getStatusUseCase().toHomeUiState()
    }.getOrElse {
        HomeUiState.Error
    }

    private fun AccountStatus.toHomeUiState(): HomeUiState = when (this) {
        is AccountStatus.Known -> {
            val copy = state.toStatusCopy()
            HomeUiState.Content(
                account = this,
                status = copy.status,
                message = copy.message,
                nextChargeLabel = copy.nextChargeLabel,
            )
        }

        AccountStatus.NoProfile -> {
            HomeUiState.NoAccount
        }
    }

    private fun AccountStatus.State.toStatusCopy(): StatusCopy = when (this) {
        AccountStatus.State.ALL_GOOD -> StatusCopy(
            status = R.string.status_all_good,
            message = R.string.status_all_good_message,
            nextChargeLabel = R.string.next_charge,
        )

        AccountStatus.State.PAYMENT_DUE -> StatusCopy(
            status = R.string.status_payment_due,
            message = R.string.status_payment_due_message,
            nextChargeLabel = R.string.due,
        )

        AccountStatus.State.UNKNOWN -> StatusCopy(
            status = R.string.status_unknown,
            message = R.string.status_unknown_message,
            nextChargeLabel = R.string.next_charge,
        )
    }
}

private data class StatusCopy(
    @StringRes val status: Int,
    @StringRes val message: Int,
    @StringRes val nextChargeLabel: Int,
)
