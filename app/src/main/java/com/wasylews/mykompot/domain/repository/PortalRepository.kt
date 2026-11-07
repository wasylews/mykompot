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

import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface PortalRepository {
    suspend fun login(
        username: String,
        password: String,
    ): LoginResult

    suspend fun loadProfile(): Profile?

    fun sessionFlow(): Flow<String?>
}
