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

import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.repository.CredentialsStore
import com.wasylews.mykompot.domain.repository.PortalRepository
import org.koin.core.annotation.Factory

/** Restores a signed-in session from remembered credentials when no session token is stored. */
fun interface RestoreSession {
    suspend operator fun invoke()
}

/**
 * Logs in with remembered credentials so returning users can skip the login screen.
 *
 * Invalid credentials are discarded. Network/server failures keep the credentials so a later
 * launch can retry.
 */
@Factory
class RestoreSessionUseCase(
    private val credentialsStore: CredentialsStore,
    private val repository: PortalRepository,
) : RestoreSession {
    override suspend fun invoke() {
        val credentials = credentialsStore.read() ?: return
        when (repository.login(credentials)) {
            is LoginResult.Success -> Unit
            LoginResult.InvalidCredentials -> credentialsStore.clear()
            is LoginResult.Failed -> Unit
        }
    }
}
