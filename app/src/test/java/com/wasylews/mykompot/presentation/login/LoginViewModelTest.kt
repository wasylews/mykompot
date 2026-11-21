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
import com.wasylews.mykompot.ProfileFixtures
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.FakeCredentialsStore
import com.wasylews.mykompot.domain.FakePaymentDueReminderScheduler
import com.wasylews.mykompot.domain.model.Credentials
import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.repository.CredentialsStore
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : BaseViewModelTest() {
    private val profile = ProfileFixtures.profile(nextPaymentDate = LocalDate.parse("2026-10-04"))

    @Test
    fun `successful login completes without error and schedules reminders`() = runTest {
        // GIVEN
        val paymentScheduler = FakePaymentDueReminderScheduler()
        val vm = loginViewModel(successLogin(profile), paymentScheduler, profile = profile)

        // WHEN
        vm.login()
        advanceUntilIdle()

        // THEN
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorId)
        assertEquals(1, paymentScheduler.scheduleCalls)
        assertEquals(profile, paymentScheduler.lastScheduledProfile)
    }

    @Test
    fun `successful opt-in login stores credentials`() = runTest {
        // GIVEN
        val store = FakeCredentialsStore()
        val vm = loginViewModel(successLogin(profile), credentialsStore = store, profile = profile)

        // WHEN
        vm.setRememberMe(true)
        vm.login()
        advanceUntilIdle()

        // THEN
        assertEquals(Credentials("user", "pass"), store.read())
        assertTrue(store.hasCredentials())
    }

    @Test
    fun `successful login without opt-in does not store credentials`() = runTest {
        // GIVEN
        val store = FakeCredentialsStore()
        val vm = loginViewModel(successLogin(profile), credentialsStore = store, profile = profile)

        // WHEN
        vm.login()
        advanceUntilIdle()

        // THEN
        assertNull(store.read())
        assertFalse(vm.uiState.value.rememberMe)
    }

    @Test
    fun `invalid credentials clear remembered credentials`() = runTest {
        // GIVEN
        val store = FakeCredentialsStore()
        store.save(Credentials("old-user", "old-pass"))
        val vm = loginViewModel(LoginResult.InvalidCredentials, credentialsStore = store)

        // WHEN
        vm.setRememberMe(true)
        vm.login()
        advanceUntilIdle()

        // THEN
        assertNull(store.read())
    }

    @Test
    fun `transient failure keeps remembered credentials`() = runTest {
        // GIVEN
        val store = FakeCredentialsStore()
        val remembered = Credentials("user", "pass")
        store.save(remembered)
        val vm = loginViewModel(LoginResult.Failed(PortalFailure.Network), credentialsStore = store)

        // WHEN
        vm.setRememberMe(true)
        vm.login()
        advanceUntilIdle()

        // THEN
        assertEquals(remembered, store.read())
    }

    @Test
    fun `remember me can be toggled`() = runTest {
        // GIVEN
        val vm = loginViewModel(successLogin(profile), profile = profile)

        // WHEN / THEN
        assertFalse(vm.uiState.value.rememberMe)
        vm.setRememberMe(true)
        assertTrue(vm.uiState.value.rememberMe)
        vm.setRememberMe(false)
        assertFalse(vm.uiState.value.rememberMe)
    }

    @Test
    fun `empty credentials are rejected without starting login`() = runTest {
        // GIVEN
        val paymentScheduler = FakePaymentDueReminderScheduler()
        val vm = LoginViewModel(
            FakePortalRepository(successLogin(profile), profile = profile),
            paymentScheduler,
            FakeCredentialsStore(),
        )

        // WHEN
        vm.login()
        advanceUntilIdle()

        // THEN
        assertFalse(vm.isUsernameValid)
        assertFalse(vm.isPasswordValid)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorId)
        assertEquals(0, paymentScheduler.scheduleCalls)
    }

    @Test
    fun `login emits loading then idle`() = runTest {
        // GIVEN
        val vm = loginViewModel(successLogin(profile), profile = profile)

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
    fun `login failure maps to error, can be cleared, and does not schedule reminders`() = runTest {
        val cases = listOf(
            LoginResult.InvalidCredentials to R.string.error_invalid_credentials,
            LoginResult.Failed(PortalFailure.Network) to R.string.error_no_connection,
            LoginResult.Failed(PortalFailure.Server) to R.string.error_server,
            LoginResult.Failed(PortalFailure.Parse) to R.string.error_parsing,
        )

        cases.forEach { (result, expectedError) ->
            // GIVEN
            val paymentScheduler = FakePaymentDueReminderScheduler()
            val vm = loginViewModel(result, paymentScheduler)

            // WHEN
            vm.login()
            advanceUntilIdle()

            // THEN
            assertEquals(expectedError, vm.uiState.value.errorId)
            assertFalse(vm.uiState.value.isLoading)
            assertEquals(0, paymentScheduler.scheduleCalls)

            vm.clearError()
            assertNull(vm.uiState.value.errorId)
        }
    }

    private fun loginViewModel(
        result: LoginResult,
        paymentScheduler: FakePaymentDueReminderScheduler = FakePaymentDueReminderScheduler(),
        profile: Profile? = this.profile,
        credentialsStore: CredentialsStore = FakeCredentialsStore(),
    ): LoginViewModel = LoginViewModel(
        FakePortalRepository(result, profile = profile),
        paymentScheduler,
        credentialsStore,
    ).apply {
        usernameState.edit { replace(0, length, "user") }
        passwordState.edit { replace(0, length, "pass") }
    }
}
