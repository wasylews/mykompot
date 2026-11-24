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
import com.wasylews.mykompot.presentation.catchAsNull
import com.wasylews.mykompot.presentation.toErrorId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
    observeStatus: ObserveStatus,
    observeSession: ObserveSession,
    private val refreshStatus: RefreshStatus,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val errorId = MutableStateFlow<Int?>(null)
    private var refreshJob: Job? = null

    private val latestStatus = observeStatus.statusFlow()
        .catchAsNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    // The ViewModel is owned by the logged-in navigation destination, but optimistic startup can
    // show Home before a remembered session is restored. Wait for a confirmed session so the
    // initial refresh does not expire against a missing cookie and strand the skeleton.
    init {
        viewModelScope.launch {
            observeSession.sessionStateFlow().first { it == SessionState.LoggedIn }
            refresh()
        }
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
                    is RefreshResult.Failed -> errorId.value = result.reason.toErrorId()
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
        observeSession.sessionStateFlow(),
        isRefreshing,
        errorId,
    ) { status, sessionState, isRefreshing, errorId ->
        if (sessionState != SessionState.LoggedIn) {
            HomeUiState()
        } else {
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
