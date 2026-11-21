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
import com.wasylews.mykompot.domain.repository.CredentialsStore
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

enum class AppRootDestination {
    Home,
    Login,
}

@KoinViewModel
class MainViewModel(
    observeSession: ObserveSession,
    private val logOutUseCase: LogOut,
    private val restoreSession: RestoreSession,
    credentialsStore: CredentialsStore,
    portalRepository: PortalRepository,
) : ViewModel() {
    private val hasCredentialsAtLaunch = credentialsStore.hasCredentials()
    private val sessionFlow = observeSession.sessionStateFlow()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val restoreFinished = MutableStateFlow(false)

    val isLoggedIn: StateFlow<Boolean> = sessionFlow
        .map { it == SessionState.LoggedIn }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            portalRepository.hasActiveSession(),
        )

    val rootDestination: StateFlow<AppRootDestination> = combine(
        sessionFlow,
        restoreFinished,
    ) { session, finished ->
        when {
            session == SessionState.LoggedIn -> AppRootDestination.Home
            !finished && hasCredentialsAtLaunch -> AppRootDestination.Home
            else -> AppRootDestination.Login
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initialRootDestination(portalRepository.hasActiveSession()),
    )

    init {
        viewModelScope.launch {
            try {
                if (sessionFlow.first() != SessionState.LoggedIn && hasCredentialsAtLaunch) {
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

    private fun initialRootDestination(hasActiveSession: Boolean): AppRootDestination = when {
        hasActiveSession || hasCredentialsAtLaunch -> AppRootDestination.Home
        else -> AppRootDestination.Login
    }
}
