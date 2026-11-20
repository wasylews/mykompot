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
import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.ObserveStatus
import com.wasylews.mykompot.domain.RefreshStatus
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
    observeStatus: ObserveStatus,
    private val refreshStatus: RefreshStatus,
    observeSession: ObserveSession,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val errorId = MutableStateFlow<Int?>(null)
    private var refreshJob: Job? = null

    private val latestStatus = observeStatus.statusFlow()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    init {
        observeSession.sessionStateFlow()
            .onEach { state ->
                if (state == SessionState.LoggedIn) {
                    refresh()
                }
            }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        errorId.value = null
    }

    fun refresh(userInitiated: Boolean = false) {
        if (userInitiated) {
            isRefreshing.value = true
        }
        if (refreshJob?.isActive == true) {
            return
        }
        errorId.value = null
        refreshJob = viewModelScope.launch {
            try {
                when (val result = refreshStatus()) {
                    is RefreshResult.Failed -> errorId.value = result.toErrorId()
                    is RefreshResult.Updated, RefreshResult.Expired -> Unit
                }
            } finally {
                refreshJob = null
                isRefreshing.value = false
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        latestStatus,
        isRefreshing,
        errorId,
    ) { status, isRefreshing, errorId ->
        HomeUiState(
            content = when {
                status != null -> status.toHomeContent()
                errorId != null -> HomeContent.Unknown
                else -> HomeContent.Loading
            },
            isRefreshing = isRefreshing,
            errorId = errorId,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState(),
        )

    private fun AccountStatus.toHomeContent(): HomeContent.Content = when (state) {
        AccountStatus.State.ALL_GOOD -> HomeContent.Content(
            account = this,
            status = R.string.status_all_good,
            message = R.string.status_all_good_message,
            nextChargeLabel = R.string.next_charge.takeIf { daysLeft != null },
        )

        AccountStatus.State.PAYMENT_DUE -> HomeContent.Content(
            account = this,
            status = R.string.status_payment_due,
            message = R.string.status_payment_due_message,
            nextChargeLabel = R.string.due.takeIf { daysLeft != null },
        )

        AccountStatus.State.SUSPENDED -> HomeContent.Content(
            account = this,
            status = R.string.status_suspended,
            message = R.string.status_suspended_message,
            nextChargeLabel = null,
        )
    }
}

private fun RefreshResult.Failed.toErrorId(): Int = when (this) {
    RefreshResult.Failed.Network -> R.string.error_no_connection

    RefreshResult.Failed.Server,
    RefreshResult.Failed.Parse,
    -> R.string.error_server
}
