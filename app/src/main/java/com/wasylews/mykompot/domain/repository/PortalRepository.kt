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
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.PaymentsActionResult
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.TariffPlans
import com.wasylews.mykompot.domain.model.TariffPlansActionResult
import kotlinx.coroutines.flow.Flow

/**
 * Access point for portal-backed account data and cached local state.
 *
 * The repository coordinates remote portal requests with local persistence. When the portal
 * indicates an expired session, session-scoped caches (profile, session, payments) are cleared;
 * tariff plans are preserved.
 */
interface PortalRepository {
    /**
     * Authenticates with the portal and, on success, stores the session and profile.
     */
    suspend fun login(
        username: String,
        password: String,
    ): LoginResult

    /**
     * Refreshes the current account profile from the portal using the stored session.
     *
     * @return [RefreshResult.Updated] on success, [RefreshResult.Expired] when the session is no
     * longer valid, or [RefreshResult.Failed] when refresh fails without expiry.
     */
    suspend fun refresh(): RefreshResult

    /** Observes the cached account profile, or `null` when unavailable. */
    fun profileFlow(): Flow<Profile?>

    /** Observes the stored session token, or `null` when logged out/expired. */
    fun sessionFlow(): Flow<String?>

    /** Observes the cached tariff plans catalog, or `null` when unavailable. */
    fun tariffPlansFlow(): Flow<TariffPlans?>

    /**
     * Refreshes the tariff plans catalog, using conditional request validators when available.
     */
    suspend fun refreshTariffPlans(): TariffPlansActionResult

    /** Observes the cached payments history, or `null` when unavailable. */
    fun paymentsFlow(): Flow<Payments?>

    /**
     * Refreshes the first page of payments history, replacing cached payments on success.
     */
    suspend fun refreshPayments(): PaymentsActionResult

    /**
     * Loads and appends the next page of payments history when more data is available.
     */
    suspend fun loadMorePayments(): PaymentsActionResult
}
