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

package com.wasylews.mykompot.presentation

import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.PaymentsActionResult
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class FakePortalRepository(
    private val result: LoginResult = successLogin(),
    private val profile: Profile? = profile(),
    private val session: String? = null,
    var refreshResult: RefreshResult = RefreshResult.Updated(profile ?: profile()),
    tariffPlans: List<TariffPlan>? = null,
    private val payments: Payments? = null,
    var paymentsRefreshResult: PaymentsActionResult = PaymentsActionResult.Success,
    var paymentsLoadMoreResult: PaymentsActionResult = PaymentsActionResult.Success,
) : PortalRepository {
    private val tariffPlans = tariffPlans?.let { TariffPlans(it) }
    var refreshCalls = 0
    var refreshTariffPlansCalls = 0
    var refreshPaymentsCalls = 0
    var loadMorePaymentsCalls = 0

    override suspend fun login(
        username: String,
        password: String,
    ): LoginResult = result

    override suspend fun refresh(): RefreshResult {
        refreshCalls++
        return refreshResult
    }

    override fun profileFlow(): Flow<Profile?> = flowOf(profile)

    override fun sessionFlow(): Flow<String?> = flowOf(session)

    override fun tariffPlansFlow(): Flow<TariffPlans?> = flowOf(tariffPlans)

    override suspend fun refreshTariffPlans() {
        refreshTariffPlansCalls++
    }

    override fun paymentsFlow(): Flow<Payments?> = flowOf(payments)

    override suspend fun refreshPayments(): PaymentsActionResult {
        refreshPaymentsCalls++
        return paymentsRefreshResult
    }

    override suspend fun loadMorePayments(): PaymentsActionResult {
        loadMorePaymentsCalls++
        return paymentsLoadMoreResult
    }
}

fun successLogin(profile: Profile = profile()) = LoginResult.Success(profile)

fun profile() = Profile(
    isActive = true,
    nextPaymentDate = LocalDate.of(2026, 10, 27),
    tariffPlan = TariffPlan(id = "300GRN_100mb", name = "Standart", speed = 100, price = 300),
)
