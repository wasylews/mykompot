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

import com.wasylews.mykompot.domain.repository.CredentialsStore
import com.wasylews.mykompot.domain.repository.PortalRepository
import org.koin.core.annotation.Factory

/** Clears all local authentication state and returns the app to the logged-out state. */
fun interface LogOut {
    suspend operator fun invoke()
}

/** Forgets the local session, remembered credentials and payment reminders. */
@Factory
class LogOutUseCase(
    private val repository: PortalRepository,
    private val credentialsStore: CredentialsStore,
    private val paymentDueReminderScheduler: PaymentDueReminderScheduler,
) : LogOut {
    override suspend fun invoke() {
        repository.logOut()
        credentialsStore.clear()
        paymentDueReminderScheduler.cancel()
    }
}
