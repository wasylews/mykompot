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

package com.wasylews.mykompot.data

import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import kotlinx.coroutines.flow.Flow

interface LocalStorage {
    suspend fun saveProfile(profile: Profile)

    /**
     * Emits the saved profile, or `null` when nothing usable is cached.
     */
    fun profileFlow(): Flow<Profile?>

    fun sessionFlow(): Flow<String?>

    suspend fun saveSession(sid: String)

    suspend fun clearProfile()

    suspend fun clearSession()

    fun tariffPlansFlow(): Flow<List<TariffPlan>?>

    suspend fun saveTariffPlans(plans: List<TariffPlan>)
}
