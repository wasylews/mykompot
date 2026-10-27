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
import com.wasylews.mykompot.presentation.FakePortalRepository
import com.wasylews.mykompot.presentation.successLogin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `can login with correct credentials`() = runTest {
        // GIVEN a repository that returns a successful login
        val vm = LoginViewModel(FakePortalRepository(successLogin()))

        // WHEN valid credentials are entered and login is invoked
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }
        vm.login()
        advanceUntilIdle()

        // THEN the state is LoggedIn
        assertEquals(LoginState.LoggedIn, vm.loginState.value)
    }

    @Test
    fun `empty credentials are validated`() {
        // GIVEN a view model with empty credentials
        val vm = LoginViewModel(FakePortalRepository(LoginResult.InvalidCredentials))
        vm.usernameState.edit { replace(0, length, "") }
        vm.passwordState.edit { replace(0, length, "") }

        // WHEN login is invoked
        vm.login()

        // THEN both username and password are invalid
        assertFalse(vm.isUsernameValid)
        assertFalse(vm.isPasswordValid)
    }

    @Test
    fun `login emits loading then logged in`() =
        runTest {
            // GIVEN a repository that returns a successful login
            val vm = LoginViewModel(FakePortalRepository(successLogin()))
            vm.usernameState.edit { replace(0, length, "user") }
            vm.passwordState.edit { replace(0, length, "pass") }

            // WHEN login is invoked
            vm.loginState.test {
                assertEquals(LoginState.Unknown, awaitItem())
                vm.login()

                // THEN we transition from loading to logged in
                assertEquals(LoginState.Loading, awaitItem())
                assertEquals(LoginState.LoggedIn, awaitItem())
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

        // THEN an invalid-credentials error state is set
        val state = vm.loginState.value as LoginState.Error
        assertEquals(R.string.error_invalid_credentials, state.errorId)
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

        // THEN a no-connection error state is set
        val state = vm.loginState.value as LoginState.Error
        assertEquals(R.string.error_no_connection, state.errorId)
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

        // THEN a server error state is set
        val state = vm.loginState.value as LoginState.Error
        assertEquals(R.string.error_server, state.errorId)
    }
}
