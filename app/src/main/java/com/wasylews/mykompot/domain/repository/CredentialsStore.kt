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

package com.wasylews.mykompot.domain.repository

import com.wasylews.mykompot.domain.model.Credentials

/**
 * Storage for credentials used by automatic re-login.
 *
 * Methods are synchronous because OkHttp's authenticator runs on a blocking dispatcher thread and
 * needs to read credentials before retrying a request.
 */
interface CredentialsStore {
    /** Saves [credentials] after a successful opt-in login. */
    fun save(credentials: Credentials)

    /** Returns the stored credentials, or `null` when the user did not opt in. */
    fun read(): Credentials?

    /** Removes stored credentials, for example on logout or definitive invalid credentials. */
    fun clear()

    /** Whether credentials are currently stored. */
    fun hasCredentials(): Boolean = read() != null
}
