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
import com.wasylews.mykompot.domain.ObserveStatus
import com.wasylews.mykompot.domain.RefreshStatus
import com.wasylews.mykompot.domain.model.AccountStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
    observeStatusUseCase: ObserveStatus,
    private val refreshStatus: RefreshStatus,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val errorId = MutableStateFlow<Int?>(null)

    init {
        refresh()
    }

    fun clearError() {
        errorId.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                refreshStatus()
            } finally {
                isRefreshing.value = false
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        observeStatusUseCase.statusFlow(),
        isRefreshing,
        errorId,
    ) { status, isRefreshing, errorId ->
        HomeUiState(
            content = status.toHomeContent(isRefreshing),
            isRefreshing = isRefreshing,
            errorId = errorId,
        )
    }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(HomeUiState(content = HomeContent.Unknown))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState(),
        )

    private fun AccountStatus?.toHomeContent(isRefreshing: Boolean): HomeContent = when (this) {
        null -> {
            if (isRefreshing) HomeContent.Loading else HomeContent.Unknown
        }

        else -> {
            val copy = state.toStatusCopy()
            HomeContent.Content(
                account = this,
                status = copy.status,
                message = copy.message,
                nextChargeLabel = copy.nextChargeLabel.takeIf { daysLeft != null },
            )
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

        AccountStatus.State.SUSPENDED -> StatusCopy(
            status = R.string.status_suspended,
            message = R.string.status_suspended_message,
            nextChargeLabel = null,
        )
    }
}

private data class StatusCopy(
    @StringRes val status: Int,
    @StringRes val message: Int,
    @StringRes val nextChargeLabel: Int?,
)
