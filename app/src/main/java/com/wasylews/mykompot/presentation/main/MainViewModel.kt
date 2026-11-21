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

package com.wasylews.mykompot.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasylews.mykompot.domain.LogOut
import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.RestoreSession
import com.wasylews.mykompot.domain.model.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainViewModel(
    observeSession: ObserveSession,
    private val logOutUseCase: LogOut,
    restoreSession: RestoreSession,
) : ViewModel() {
    private val sessionFlow = observeSession.sessionStateFlow()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val restoreFinished = MutableStateFlow(false)

    val sessionState: StateFlow<SessionState> = combine(
        sessionFlow,
        restoreFinished,
    ) { session, finished ->
        when {
            session == SessionState.LoggedIn -> SessionState.LoggedIn
            !finished -> SessionState.Loading
            else -> SessionState.LoggedOut
        }
    }
        .catch { emit(SessionState.LoggedOut) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    init {
        viewModelScope.launch {
            try {
                if (sessionFlow.first() != SessionState.LoggedIn) {
                    restoreSession()
                }
            } finally {
                restoreFinished.value = true
            }
        }
    }

    fun logOut() = viewModelScope.launch {
        logOutUseCase()
    }
}
