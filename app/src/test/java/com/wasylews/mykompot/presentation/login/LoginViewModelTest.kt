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
    fun `successful login completes without error`() = runTest {
        // GIVEN
        val vm = loginViewModel(successLogin())

        // WHEN
        vm.login()
        advanceUntilIdle()

        // THEN
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorId)
    }

    @Test
    fun `empty credentials are rejected without starting login`() = runTest {
        // GIVEN
        val vm = LoginViewModel(FakePortalRepository(successLogin()))

        // WHEN
        vm.login()
        advanceUntilIdle()

        // THEN
        assertFalse(vm.isUsernameValid)
        assertFalse(vm.isPasswordValid)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorId)
    }

    @Test
    fun `login emits loading then idle`() = runTest {
        // GIVEN
        val vm = loginViewModel(successLogin())

        vm.uiState.test {
            assertFalse(awaitItem().isLoading)

            // WHEN
            vm.login()

            // THEN
            assertTrue(awaitItem().isLoading)
            assertFalse(awaitItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login failure maps to error and can be cleared`() = runTest {
        val cases = listOf(
            LoginResult.InvalidCredentials to R.string.error_invalid_credentials,
            LoginResult.NetworkError to R.string.error_no_connection,
            LoginResult.ServerError to R.string.error_server,
            LoginResult.ParsingError to R.string.error_parsing,
        )

        cases.forEach { (result, expectedError) ->
            // GIVEN
            val vm = loginViewModel(result)

            // WHEN
            vm.login()
            advanceUntilIdle()

            // THEN
            assertEquals(expectedError, vm.uiState.value.errorId)
            assertFalse(vm.uiState.value.isLoading)

            vm.clearError()
            assertNull(vm.uiState.value.errorId)
        }
    }

    private fun loginViewModel(result: LoginResult): LoginViewModel = LoginViewModel(FakePortalRepository(result)).apply {
        usernameState.edit { replace(0, length, "user") }
        passwordState.edit { replace(0, length, "pass") }
    }
}
