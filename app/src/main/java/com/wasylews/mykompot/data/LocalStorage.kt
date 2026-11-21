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

import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlans
import kotlinx.coroutines.flow.Flow

/**
 * Local persistence for session-scoped and cached portal data.
 *
 * `null` emissions indicate that no usable value is stored for the given data.
 */
interface LocalStorage {
    /** Persists the latest account [profile]. */
    suspend fun saveProfile(profile: Profile)

    /** Emits the saved profile, or `null` when nothing usable is cached. */
    fun profileFlow(): Flow<Profile?>

    /** Removes the cached profile. */
    suspend fun clearProfile()

    /** Emits the cached tariff plans catalog, or `null` when absent. */
    fun tariffPlansFlow(): Flow<TariffPlans?>

    /** Persists the tariff plans catalog (including cache validators). */
    suspend fun saveTariffPlans(tariffPlans: TariffPlans)

    /** Emits cached payments history, or `null` when absent. */
    fun paymentsFlow(): Flow<Payments?>

    /** Persists payments history. */
    suspend fun savePayments(payments: Payments)

    /** Removes cached payments history. */
    suspend fun clearPayments()
}
