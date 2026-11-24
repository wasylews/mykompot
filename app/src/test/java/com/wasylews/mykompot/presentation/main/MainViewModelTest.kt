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

import app.cash.turbine.test
import com.wasylews.mykompot.domain.FakeCredentialsStore
import com.wasylews.mykompot.domain.LogOut
import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.RestoreSession
import com.wasylews.mykompot.domain.model.Credentials
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.domain.repository.PortalRepository
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.FakePortalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : BaseViewModelTest() {
    @Test
    fun `shows login immediately when no session and no credentials`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedOut)
        val restoreSession = FakeRestoreSession()
        val vm = MainViewModel(
            observeSession = observeSession,
            logOutUseCase = FakeLogOut(),
            restoreSession = restoreSession,
            credentialsStore = FakeCredentialsStore(),
            portalRepository = FakePortalRepository(),
        )

        advanceUntilIdle()

        assertEquals(AppRootDestination.Login, vm.rootDestination.value)
        assertFalse(vm.isLoggedIn.value)
        assertEquals(0, restoreSession.calls)
    }

    @Test
    fun `shows home optimistically while restoring remembered credentials`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedOut)
        val restoreSession = FakeRestoreSession()
        val vm = MainViewModel(
            observeSession = observeSession,
            logOutUseCase = FakeLogOut(),
            restoreSession = restoreSession,
            credentialsStore = FakeCredentialsStore().apply { save(Credentials("user", "password")) },
            portalRepository = FakePortalRepository(),
        )

        assertEquals(AppRootDestination.Home, vm.rootDestination.value)
        assertFalse(vm.isLoggedIn.value)

        advanceUntilIdle()

        assertEquals(1, restoreSession.calls)
        assertEquals(AppRootDestination.Login, vm.rootDestination.value)
    }

    @Test
    fun `shows home after restore creates a session`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedOut)
        val restoreSession = FakeRestoreSession().apply {
            onInvoke = { observeSession.setState(SessionState.LoggedIn) }
        }
        val vm = MainViewModel(
            observeSession = observeSession,
            logOutUseCase = FakeLogOut(),
            restoreSession = restoreSession,
            credentialsStore = FakeCredentialsStore().apply { save(Credentials("user", "password")) },
            portalRepository = FakePortalRepository(),
        )

        advanceUntilIdle()

        assertEquals(AppRootDestination.Home, vm.rootDestination.value)
        assertTrue(vm.isLoggedIn.value)
    }

    @Test
    fun `shows home immediately when session exists`() = runTest {
        val restoreSession = FakeRestoreSession()
        val vm = MainViewModel(
            observeSession = FakeObserveSession(SessionState.LoggedIn),
            logOutUseCase = FakeLogOut(),
            restoreSession = restoreSession,
            credentialsStore = FakeCredentialsStore(),
            portalRepository = FakePortalRepository(session = "sid"),
        )

        advanceUntilIdle()

        assertEquals(AppRootDestination.Home, vm.rootDestination.value)
        assertTrue(vm.isLoggedIn.value)
        assertEquals(0, restoreSession.calls)
    }

    @Test
    fun `routes to login when confirmed session is lost`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedIn)
        val portalRepository = MutableSessionRepository("sid")
        val vm = MainViewModel(
            observeSession = observeSession,
            logOutUseCase = FakeLogOut(),
            restoreSession = FakeRestoreSession(),
            credentialsStore = FakeCredentialsStore(),
            portalRepository = portalRepository,
        )

        vm.rootDestination.test {
            assertEquals(AppRootDestination.Home, awaitItem())

            portalRepository.setSession(null)
            observeSession.setState(SessionState.LoggedOut)
            advanceUntilIdle()

            assertEquals(AppRootDestination.Login, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does not flash login when restored session reaches the flow after restore returns`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedOut)
        val portalRepository = MutableSessionRepository()
        val restoreSession = FakeRestoreSession().apply {
            onInvoke = {
                // The sid store is updated synchronously, but the session flow reaches routing on
                // the next dispatcher turn (login runs on IO, routing collects on Main).
                portalRepository.setSession("restored-session")
                launch { observeSession.setState(SessionState.LoggedIn) }
            }
        }
        val vm = MainViewModel(
            observeSession = observeSession,
            logOutUseCase = FakeLogOut(),
            restoreSession = restoreSession,
            credentialsStore = FakeCredentialsStore().apply { save(Credentials("user", "password")) },
            portalRepository = portalRepository,
        )

        vm.rootDestination.test {
            assertEquals(AppRootDestination.Home, awaitItem())

            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `logout invokes log out use case`() = runTest {
        val logOut = FakeLogOut()
        val vm = MainViewModel(
            observeSession = FakeObserveSession(SessionState.LoggedIn),
            logOutUseCase = logOut,
            restoreSession = FakeRestoreSession(),
            credentialsStore = FakeCredentialsStore(),
            portalRepository = FakePortalRepository(session = "sid"),
        )

        vm.logOut()
        advanceUntilIdle()

        assertEquals(1, logOut.calls)
    }
}

class FakeLogOut : LogOut {
    var calls = 0

    override suspend fun invoke() {
        calls++
    }
}

class FakeRestoreSession : RestoreSession {
    var calls = 0
    var onInvoke: suspend () -> Unit = {}

    override suspend fun invoke() {
        calls++
        onInvoke()
    }
}

class FakeObserveSession(
    state: SessionState = SessionState.LoggedOut,
) : ObserveSession {
    private val stateFlow = MutableStateFlow(state)

    fun setState(state: SessionState) {
        stateFlow.value = state
    }

    override fun sessionStateFlow(): Flow<SessionState> = stateFlow
}

/** Portal repository whose sid store and observed flow can change independently, like restore. */
private class MutableSessionRepository(
    session: String? = null,
) : PortalRepository by FakePortalRepository() {
    private val sid = MutableStateFlow(session)

    fun setSession(value: String?) {
        sid.value = value
    }

    override fun sessionFlow(): Flow<String?> = sid.asStateFlow()

    override fun hasActiveSession(): Boolean = !sid.value.isNullOrEmpty()
}
