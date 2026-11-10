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

package com.wasylews.mykompot.presentation.login

import app.cash.turbine.test
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.presentation.BaseViewModelTest
import com.wasylews.mykompot.presentation.FakePortalRepository
import com.wasylews.mykompot.presentation.successLogin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : BaseViewModelTest() {
    @Test
    fun `can login with correct credentials`() = runTest {
        // GIVEN a repository that returns a successful login
        val vm = LoginViewModel(FakePortalRepository(successLogin()))

        // WHEN valid credentials are entered and login is invoked
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }
        vm.login()
        advanceUntilIdle()

        // THEN login completes without an error
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorId)
    }

    @Test
    fun `empty credentials are validated`() = runTest {
        // GIVEN a view model with empty credentials
        val vm = LoginViewModel(FakePortalRepository(successLogin()))

        // WHEN login is invoked
        vm.login()
        advanceUntilIdle()

        // THEN credentials are invalid and login does not start
        assertFalse(vm.isUsernameValid)
        assertFalse(vm.isPasswordValid)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorId)
    }

    @Test
    fun `login emits loading then completes`() = runTest {
        // GIVEN a repository that returns a successful login
        val vm = LoginViewModel(FakePortalRepository(successLogin()))
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }

        // WHEN login is invoked
        vm.uiState.test {
            assertFalse(awaitItem().isLoading)
            vm.login()

            // THEN we transition from loading to idle
            assertTrue(awaitItem().isLoading)
            assertFalse(awaitItem().isLoading)
        }
    }

    @Test
    fun `invalid credentials set error state`() = runTest {
        // GIVEN a repository that returns invalid credentials
        val vm = LoginViewModel(FakePortalRepository(LoginResult.InvalidCredentials))
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }

        // WHEN login is invoked
        vm.login()
        advanceUntilIdle()

        // THEN an invalid-credentials error is shown and can be consumed
        assertEquals(R.string.error_invalid_credentials, vm.uiState.value.errorId)
        assertFalse(vm.uiState.value.isLoading)
        vm.clearError()
        assertNull(vm.uiState.value.errorId)
    }

    @Test
    fun `network error sets error state`() = runTest {
        // GIVEN a repository that returns a network error
        val vm = LoginViewModel(FakePortalRepository(LoginResult.NetworkError))
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }

        // WHEN login is invoked
        vm.login()
        advanceUntilIdle()

        // THEN a no-connection error is shown and can be consumed
        assertEquals(R.string.error_no_connection, vm.uiState.value.errorId)
        assertFalse(vm.uiState.value.isLoading)
        vm.clearError()
        assertNull(vm.uiState.value.errorId)
    }

    @Test
    fun `server error sets error state`() = runTest {
        // GIVEN a repository that returns a server error
        val vm = LoginViewModel(FakePortalRepository(LoginResult.ServerError))
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }

        // WHEN login is invoked
        vm.login()
        advanceUntilIdle()

        // THEN a server error is shown and can be consumed
        assertEquals(R.string.error_server, vm.uiState.value.errorId)
        assertFalse(vm.uiState.value.isLoading)
        vm.clearError()
        assertNull(vm.uiState.value.errorId)
    }
}
