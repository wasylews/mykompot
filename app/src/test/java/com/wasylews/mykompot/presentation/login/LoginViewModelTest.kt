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
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FakeLoginRepository(
    private val result: LoginResult = LoginResult.Success,
) : PortalRepository {
    override fun login(
        username: String,
        password: String,
    ): LoginResult = result
}

class LoginViewModelTest {
    @Test
    fun `can login with correct credentials`() {
        val vm = LoginViewModel(FakeLoginRepository(LoginResult.Success))
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }

        vm.login()

        assertEquals(LoginState.LoggedIn, vm.loginState.value)
    }

    @Test
    fun `empty credentials are validated`() {
        val vm = LoginViewModel(FakeLoginRepository())
        vm.usernameState.edit { replace(0, length, "") }
        vm.passwordState.edit { replace(0, length, "") }

        vm.login()

        assertFalse(vm.isUsernameValid)
        assertFalse(vm.isPasswordValid)
    }

    @Test
    fun `login emits loading then logged in`() =
        runTest {
            val vm = LoginViewModel(FakeLoginRepository(LoginResult.Success))
            vm.usernameState.edit { replace(0, length, "user") }
            vm.passwordState.edit { replace(0, length, "pass") }

            vm.loginState.test {
                assertEquals(LoginState.Unknown, awaitItem())
                vm.login()
                assertEquals(LoginState.Loading, awaitItem())
                assertEquals(LoginState.LoggedIn, awaitItem())
            }
        }

    @Test
    fun `invalid credentials set error state`() {
        val vm = LoginViewModel(FakeLoginRepository(LoginResult.InvalidCredentials))
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }

        vm.login()

        val state = vm.loginState.value as LoginState.Error
        assertEquals(R.string.error_invalid_credentials, state.errorId)
    }

    @Test
    fun `network error sets error state`() {
        val vm = LoginViewModel(FakeLoginRepository(LoginResult.NetworkError))
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }

        vm.login()

        val state = vm.loginState.value as LoginState.Error
        assertEquals(R.string.error_no_connection, state.errorId)
    }

    @Test
    fun `server error sets error state`() {
        val vm = LoginViewModel(FakeLoginRepository(LoginResult.ServerError))
        vm.usernameState.edit { replace(0, length, "user") }
        vm.passwordState.edit { replace(0, length, "pass") }

        vm.login()

        val state = vm.loginState.value as LoginState.Error
        assertEquals(R.string.error_server, state.errorId)
    }
}
