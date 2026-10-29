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
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.repository.PortalRepository
import java.time.LocalDate

class FakePortalRepository(
    private val result: LoginResult = successLogin(),
    private val profile: Profile = profile(),
) : PortalRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): LoginResult = result

    override suspend fun loadProfile(): Profile = profile
}

fun successLogin() = LoginResult.Success

fun profile() = Profile(
    isActive = true,
    nextPaymentDate = LocalDate.of(2026, 10, 27),
    tariffPlan = TariffPlan("Standart", 100, 300),
)
