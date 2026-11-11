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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.IOException
import org.koin.core.annotation.Singleton
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate

@Singleton
class PortalRepositoryImpl(
    private val dispatcher: CoroutineDispatcher,
    private val api: PortalApi,
    private val localStorage: LocalStorage,
) : PortalRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): LoginResult = withContext(dispatcher) {
        return@withContext try {
            val response = api.login(username, password)
            if (!response.isSuccessful) return@withContext LoginResult.ServerError
            val page = response.body() ?: return@withContext LoginResult.ParsingError
            if (page.error != null) {
                return@withContext LoginResult.InvalidCredentials
            }

            val profile = page.toProfile() ?: return@withContext LoginResult.ParsingError
            val sid = response.sid() ?: page.sid ?: return@withContext LoginResult.ParsingError
            localStorage.saveProfile(profile)
            localStorage.saveSession(sid)
            LoginResult.Success
        } catch (_: HttpException) {
            LoginResult.ServerError
        } catch (_: IOException) {
            LoginResult.NetworkError
        }
    }

    private fun ProfilePageResponse.toProfile(): Profile? = Profile(
        isActive = accountState == STATUS_ACTIVE,
        nextPaymentDate = getPaymentDate(nextPayment),
        tariffPlan = getTariffPlan(plan) ?: return null,
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

    private fun Response<ProfilePageResponse>.sid(): String? = headers()
        .values("Set-Cookie")
        .firstNotNullOfOrNull { it.cookieValue(SID_COOKIE) }

    private fun String.cookieValue(name: String): String? = substringAfter(
        delimiter = "$name=",
        missingDelimiterValue = "",
    ).substringBefore(';').takeIf { it.isNotBlank() }

    override suspend fun refresh() = withContext(dispatcher) {
        try {
            val session = sessionFlow().first() ?: return@withContext
            val response = api.fetchProfile(session)
            if (!response.isSuccessful) return@withContext

            val page = response.body() ?: return@withContext
            val profile = page.toProfile() ?: return@withContext
            localStorage.saveProfile(profile)
        } catch (_: HttpException) {
        } catch (_: IOException) {
        }
    }

    override fun profileFlow(): Flow<Profile?> = localStorage.profileFlow()

    override fun sessionFlow(): Flow<String?> = localStorage.sessionFlow()

    private companion object {
        const val STATUS_ACTIVE = "Enable"
        const val SID_COOKIE = "sid"
    }
}
