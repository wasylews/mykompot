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

package com.wasylews.mykompot.domain

import com.wasylews.mykompot.domain.model.Credentials
import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.presentation.FakePortalRepository
import com.wasylews.mykompot.presentation.profile
import com.wasylews.mykompot.presentation.successLogin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreSessionUseCaseTest {
    @Test
    fun `restores remembered credentials and schedules reminders`() = runTest {
        val credentials = Credentials("user", "pass")
        val credentialsStore = FakeCredentialsStore().apply { save(credentials) }
        val profile = profile()
        val repository = FakePortalRepository(result = successLogin(profile))
        val scheduler = FakePaymentDueReminderScheduler()

        RestoreSessionUseCase(credentialsStore, repository, scheduler).invoke()

        assertEquals(1, repository.loginCalls)
        assertEquals(credentials.username to credentials.password, repository.lastLogin)
        assertEquals(1, scheduler.scheduleCalls)
        assertEquals(profile, scheduler.lastScheduledProfile)
        assertEquals(credentials, credentialsStore.read())
    }

    @Test
    fun `does nothing without remembered credentials`() = runTest {
        val repository = FakePortalRepository()
        val scheduler = FakePaymentDueReminderScheduler()

        RestoreSessionUseCase(FakeCredentialsStore(), repository, scheduler).invoke()

        assertEquals(0, repository.loginCalls)
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun `discards credentials rejected by the portal`() = runTest {
        val credentialsStore = FakeCredentialsStore().apply {
            save(Credentials("bad-user", "bad-pass"))
        }
        val repository = FakePortalRepository(result = LoginResult.InvalidCredentials)
        val scheduler = FakePaymentDueReminderScheduler()

        RestoreSessionUseCase(credentialsStore, repository, scheduler).invoke()

        assertEquals(1, repository.loginCalls)
        assertEquals(null, credentialsStore.read())
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun `keeps credentials when the portal is unavailable`() = runTest {
        val credentials = Credentials("user", "pass")
        val credentialsStore = FakeCredentialsStore().apply { save(credentials) }
        val repository = FakePortalRepository(result = LoginResult.Failed(PortalFailure.Network))
        val scheduler = FakePaymentDueReminderScheduler()

        RestoreSessionUseCase(credentialsStore, repository, scheduler).invoke()

        assertEquals(1, repository.loginCalls)
        assertEquals(credentials, credentialsStore.read())
        assertEquals(0, scheduler.scheduleCalls)
    }
}
