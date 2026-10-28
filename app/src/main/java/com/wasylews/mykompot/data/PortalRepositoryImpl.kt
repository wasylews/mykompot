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

import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.IOException
import org.koin.core.annotation.Singleton
import retrofit2.HttpException
import java.time.LocalDate

@Singleton
class PortalRepositoryImpl(
    private val dispatcher: CoroutineDispatcher,
    private val api: PortalApi,
    private val localStorage: LocalStorage
) : PortalRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): LoginResult = withContext(dispatcher) {
        return@withContext try {
            val page = api.login(username, password)
            if (page.error != null) {
                LoginResult.InvalidCredentials
            } else {
                localStorage.saveProfile(page.toProfile())
                LoginResult.Success
            }
        } catch (_: HttpException) {
            LoginResult.ServerError
        } catch (_: IOException) {
            LoginResult.NetworkError
        }
    }

    private fun ProfilePageResponse.toProfile() = Profile(
        isActive = status == STATUS_ACTIVE,
        nextPaymentDate = getPaymentDate(nextPayment) ?: LocalDate.now(),
    )

    private fun getPaymentDate(text: String?) = text?.let {
        Regex("""\((\d{4}-\d{2}-\d{2})\)""")
            .find(it)
            ?.groupValues
            ?.get(1)
            ?.let(LocalDate::parse)
    }

    private fun getTariffPlan(id: String?): TariffPlan? = id?.let {
        TariffPlan(
            name = it,
            speed = 100,
            price = 300,
        )
    }

    override suspend fun loadProfile(): Profile = localStorage.loadProfile()

    private companion object {
        const val STATUS_ACTIVE = "Enable"
    }
}
