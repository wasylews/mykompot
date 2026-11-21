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
import com.wasylews.mykompot.domain.LogOut
import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.RestoreSession
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.presentation.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : BaseViewModelTest() {
    @Test
    fun `starts in loading then resolves to logged in`() = runTest {
        val observeSession = FakeObserveSession()
        val vm = MainViewModel(observeSession, FakeLogOut(), FakeRestoreSession())

        vm.sessionState.test {
            assertEquals(SessionState.Loading, awaitItem())

            observeSession.setState(SessionState.LoggedIn)
            advanceUntilIdle()

            assertEquals(SessionState.LoggedIn, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `starts in loading then resolves to logged out`() = runTest {
        val observeSession = FakeObserveSession()
        val vm = MainViewModel(observeSession, FakeLogOut(), FakeRestoreSession())

        vm.sessionState.test {
            assertEquals(SessionState.Loading, awaitItem())

            observeSession.setState(SessionState.LoggedOut)
            advanceUntilIdle()

            assertEquals(SessionState.LoggedOut, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updates session state when session changes`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedIn)
        val vm = MainViewModel(observeSession, FakeLogOut(), FakeRestoreSession())

        vm.sessionState.test {
            assertEquals(SessionState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(SessionState.LoggedIn, expectMostRecentItem())

            observeSession.setState(SessionState.LoggedOut)
            advanceUntilIdle()
            assertEquals(SessionState.LoggedOut, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `waits for session restore before resolving logged out`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedOut)
        val restoreSession = FakeRestoreSession()
        val vm = MainViewModel(observeSession, FakeLogOut(), restoreSession)

        advanceUntilIdle()

        assertEquals(1, restoreSession.calls)
        assertEquals(SessionState.LoggedOut, vm.sessionState.value)
    }

    @Test
    fun `shows logged in after restore creates a session`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedOut)
        val restoreSession = FakeRestoreSession().apply {
            onInvoke = { observeSession.setState(SessionState.LoggedIn) }
        }
        val vm = MainViewModel(observeSession, FakeLogOut(), restoreSession)

        advanceUntilIdle()

        assertEquals(1, restoreSession.calls)
        assertEquals(SessionState.LoggedIn, vm.sessionState.value)
    }

    @Test
    fun `skips restore when a session already exists`() = runTest {
        val observeSession = FakeObserveSession(SessionState.LoggedIn)
        val restoreSession = FakeRestoreSession()
        val vm = MainViewModel(observeSession, FakeLogOut(), restoreSession)

        advanceUntilIdle()

        assertEquals(0, restoreSession.calls)
        assertEquals(SessionState.LoggedIn, vm.sessionState.value)
    }

    @Test
    fun `logout invokes log out use case`() = runTest {
        val logOut = FakeLogOut()
        val vm = MainViewModel(FakeObserveSession(), logOut, FakeRestoreSession())

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
    var onInvoke: () -> Unit = {}

    override suspend fun invoke() {
        calls++
        onInvoke()
    }
}

class FakeObserveSession(
    state: SessionState = SessionState.Loading,
) : ObserveSession {
    private val stateFlow = MutableStateFlow(state)

    fun setState(state: SessionState) {
        stateFlow.value = state
    }

    override fun sessionStateFlow(): Flow<SessionState> = stateFlow
}
