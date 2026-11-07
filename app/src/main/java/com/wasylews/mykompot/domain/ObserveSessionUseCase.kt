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

import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

interface ObserveSession {

    fun sessionStateFlow(): Flow<SessionState>
}

@Factory
class ObserveSessionUseCase(
    private val repository: PortalRepository
) : ObserveSession {

    override fun sessionStateFlow(): Flow<SessionState> = repository.sessionFlow()
        .map {
            if (it.isNullOrEmpty()) {
                SessionState.LoggedOut
            } else {
                SessionState.LoggedIn
            }
        }

}
