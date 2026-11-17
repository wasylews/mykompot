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
import com.wasylews.mykompot.domain.model.Payment
import com.wasylews.mykompot.domain.model.PaymentSource
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.PaymentsActionResult
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
            LoginResult.Success(profile)
        } catch (_: HttpException) {
            LoginResult.ServerError
        } catch (_: IOException) {
            LoginResult.NetworkError
        }
    }

    private suspend fun ProfilePageResponse.toProfile(): Profile? = Profile(
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

    private suspend fun getTariffPlan(plan: String?): TariffPlan? = plan?.let {
        val match = Regex("""(\d+)GRN_(\d+)mb""").find(it)
        val id = match?.value ?: it
        val catalog = localStorage.tariffPlansFlow().first()
        catalog?.plans?.firstOrNull { it.id == id } ?: TariffPlan(
            id = id,
            name = id,
            speed = match?.groupValues?.get(2)?.toInt() ?: 100,
            price = match?.groupValues?.get(1)?.toInt() ?: 300,
        )
    }

    private fun Response<ProfilePageResponse>.sid(): String? = headers()
        .values("Set-Cookie")
        .firstNotNullOfOrNull { it.cookieValue(SID_COOKIE) }

    private fun String.cookieValue(name: String): String? = substringAfter(
        delimiter = "$name=",
        missingDelimiterValue = "",
    ).substringBefore(';').takeIf { it.isNotBlank() }

    override suspend fun refresh(): RefreshResult = withContext(dispatcher) {
        try {
            val session = sessionFlow().first() ?: return@withContext RefreshResult.Expired
            val response = api.fetchProfile(session)
            if (!response.isSuccessful) return@withContext RefreshResult.Failed.Server

            val page = response.body() ?: return@withContext RefreshResult.Failed.Parse
            if (page.error != null) {
                localStorage.clearProfile()
                localStorage.clearSession()
                return@withContext RefreshResult.Expired
            }

            val profile = page.toProfile() ?: return@withContext RefreshResult.Failed.Parse
            val sid = response.sid() ?: page.sid
            localStorage.saveProfile(profile)
            if (sid != null) {
                localStorage.saveSession(sid)
            }
            RefreshResult.Updated(profile)
        } catch (_: HttpException) {
            RefreshResult.Failed.Server
        } catch (_: IOException) {
            RefreshResult.Failed.Network
        }
    }

    override fun profileFlow(): Flow<Profile?> = localStorage.profileFlow()

    override fun sessionFlow(): Flow<String?> = localStorage.sessionFlow()

    override fun tariffPlansFlow(): Flow<TariffPlans?> = localStorage.tariffPlansFlow()

    override suspend fun refreshTariffPlans() = withContext(dispatcher) {
        val cached = localStorage.tariffPlansFlow().first()
        val response = api.fetchPlans(
            ifNoneMatch = cached?.etag,
            ifModifiedSince = cached?.lastModified,
        )
        if (response.code() == 304) return@withContext
        if (!response.isSuccessful) throw HttpException(response)

        val items = response.body()?.items ?: throw IOException("Missing tariff plans")
        val plans = items
            .filter { "міс" in it.price }
            .map { item ->
                val price = Regex("""(\d+)""").find(item.price)?.value?.toInt() ?: 0
                TariffPlan(
                    id = "${price}GRN_${item.speed}mb",
                    name = item.name,
                    speed = item.speed,
                    price = price,
                )
            }
        localStorage.saveTariffPlans(
            TariffPlans(
                plans = plans,
                etag = response.headers()["ETag"],
                lastModified = response.headers()["Last-Modified"],
            ),
        )
    }

    override fun paymentsFlow(): Flow<Payments?> = localStorage.paymentsFlow()

    override suspend fun refreshPayments(): PaymentsActionResult = withContext(dispatcher) {
        fetchPayments(pg = null, append = false)
    }

    override suspend fun loadMorePayments(): PaymentsActionResult = withContext(dispatcher) {
        val cached = localStorage.paymentsFlow().first() ?: return@withContext PaymentsActionResult.Success
        if (!cached.hasMore) return@withContext PaymentsActionResult.Success
        fetchPayments(pg = cached.items.size, append = true)
    }

    private suspend fun fetchPayments(
        pg: Int?,
        append: Boolean,
    ): PaymentsActionResult = try {
        val session = sessionFlow().first()
        when {
            session == null -> PaymentsActionResult.Expired

            else -> {
                val response = api.fetchFees(session, pg)
                when {
                    !response.isSuccessful -> PaymentsActionResult.Failed.Server
                    else -> handleFeesResponse(response.body(), append)
                }
            }
        }
    } catch (_: HttpException) {
        PaymentsActionResult.Failed.Server
    } catch (_: IOException) {
        PaymentsActionResult.Failed.Network
    }

    private suspend fun handleFeesResponse(
        page: FeesPageResponse?,
        append: Boolean,
    ): PaymentsActionResult = when {
        page == null -> PaymentsActionResult.Failed.Parse

        page.error != null -> {
            localStorage.clearProfile()
            localStorage.clearSession()
            localStorage.clearPayments()
            PaymentsActionResult.Expired
        }

        else -> {
            val catalog = localStorage.tariffPlansFlow().first()?.plans.orEmpty()
            val payments = page.items.mapNotNull { it.toDomain(catalog) }
            if (page.items.isNotEmpty() && payments.isEmpty()) {
                PaymentsActionResult.Failed.Parse
            } else {
                val merged = if (append) {
                    localStorage.paymentsFlow().first()?.items.orEmpty() + payments
                } else {
                    payments
                }
                localStorage.savePayments(
                    Payments(
                        items = merged,
                        hasMore = payments.size == PAGE_SIZE,
                    ),
                )
                PaymentsActionResult.Success
            }
        }
    }

    private fun ParsedFeeItem.toDomain(catalog: List<TariffPlan>): Payment? {
        val dateTime = runCatching {
            LocalDateTime.parse(date, FEES_DATE_FORMATTER)
        }.getOrNull() ?: return null
        val amount = sum.trim().toIntOrNull() ?: return null
        val tariffName = catalog.firstOrNull { it.price == amount }?.name ?: amount.toString()
        return Payment(
            dateTime = dateTime,
            amount = amount,
            tariffName = tariffName,
            source = description.toPaymentSource(),
        )
    }

    private fun String.toPaymentSource(): PaymentSource = when {
        "abonplata_privatbank" in this -> PaymentSource.PRIVAT24
        "abonplata_easypay" in this -> PaymentSource.EASYPAY
        "abonplata_monobank" in this -> PaymentSource.MONOBANK
        else -> PaymentSource.UNKNOWN
    }

    private companion object {
        const val STATUS_ACTIVE = "Enable"
        const val SID_COOKIE = "sid"
        const val PAGE_SIZE = 25
        val FEES_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
