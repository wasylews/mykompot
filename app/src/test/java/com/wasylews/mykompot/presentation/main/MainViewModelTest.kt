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

import com.wasylews.mykompot.domain.ObserveSession
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.presentation.BaseViewModelTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : BaseViewModelTest() {
    @Test
    fun `async stored session resolves to logged in`() = runTest {
        val observeSession = FakeObserveSession()
        val vm = MainViewModel(observeSession)

        advanceUntilIdle()
        assertEquals(SessionState.Loading, vm.sessionState.value)

        observeSession.setState(SessionState.LoggedIn)
        advanceUntilIdle()

        assertEquals(SessionState.LoggedIn, vm.sessionState.value)
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
