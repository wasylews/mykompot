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
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import com.wasylews.mykompot.domain.model.TariffPlansActionResult
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.IOException
import org.koin.core.annotation.Singleton
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Portal-backed [PortalRepository] implementation.
 *
 * Handles login/profile/fees/plans requests, maps portal HTML responses to domain models, and
 * persists cached data in [LocalStorage]. Requests are performed on [dispatcher].
 *
 * Session expiry is detected when the portal serves its login page; in that case session-scoped
 * caches (profile, session, payments) are cleared while tariff plans are kept. Generic portal
 * errors and parsing/network failures do not clear existing caches.
 */
@Suppress("TooManyFunctions")
@Singleton
class PortalRepositoryImpl(
    private val dispatcher: CoroutineDispatcher,
    private val authApi: AuthApi,
    private val portalApi: PortalApi,
    private val plansApi: PlansApi,
    private val localStorage: LocalStorage,
    private val sessionCookieStore: SessionCookieStore,
) : PortalRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): LoginResult = withContext(dispatcher) {
        return@withContext try {
            val response = authApi.login(username, password, PORTAL_LANGUAGE)
            if (!response.isSuccessful) return@withContext LoginResult.Failed(PortalFailure.Server)
            val page = response.body() ?: return@withContext LoginResult.Failed(PortalFailure.Parse)
            if (page.isLoginPage) {
                return@withContext LoginResult.InvalidCredentials
            }
            if (page.error != null) {
                return@withContext LoginResult.Failed(PortalFailure.Server)
            }

            val profile = page.toProfile() ?: return@withContext LoginResult.Failed(PortalFailure.Parse)
            page.sid?.trim()?.takeIf { it.isNotEmpty() }?.let(sessionCookieStore::saveSid)
            localStorage.saveProfile(profile)
            LoginResult.Success(profile)
        } catch (_: HttpException) {
            LoginResult.Failed(PortalFailure.Server)
        } catch (_: IOException) {
            LoginResult.Failed(PortalFailure.Network)
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

    override suspend fun refresh(): RefreshResult = withContext(dispatcher) {
        try {
            val response = portalApi.fetchProfile()
            if (response.code() == HTTP_UNAUTHORIZED) {
                clearSessionCaches()
                return@withContext RefreshResult.Expired
            }
            if (!response.isSuccessful) return@withContext RefreshResult.Failed(PortalFailure.Server)

            val page = response.body() ?: return@withContext RefreshResult.Failed(PortalFailure.Parse)
            if (page.isLoginPage) {
                clearSessionCaches()
                return@withContext RefreshResult.Expired
            }
            if (page.error != null) return@withContext RefreshResult.Failed(PortalFailure.Server)

            val profile = page.toProfile() ?: return@withContext RefreshResult.Failed(PortalFailure.Parse)
            localStorage.saveProfile(profile)
            RefreshResult.Updated(profile)
        } catch (_: HttpException) {
            RefreshResult.Failed(PortalFailure.Server)
        } catch (_: IOException) {
            RefreshResult.Failed(PortalFailure.Network)
        }
    }

    override suspend fun logOut() = withContext(dispatcher) {
        clearSessionCaches()
    }

    override fun profileFlow(): Flow<Profile?> = localStorage.profileFlow()

    override fun sessionFlow(): Flow<String?> = sessionCookieStore.sidFlow()

    override fun hasActiveSession(): Boolean = !sessionCookieStore.sid().isNullOrEmpty()

    override fun tariffPlansFlow(): Flow<TariffPlans?> = localStorage.tariffPlansFlow()

    override suspend fun refreshTariffPlans(): TariffPlansActionResult = withContext(dispatcher) {
        try {
            val cached = localStorage.tariffPlansFlow().first()
            val response = plansApi.fetchPlans(
                ifNoneMatch = cached?.etag,
                ifModifiedSince = cached?.lastModified,
            )
            if (response.code() == 304) return@withContext TariffPlansActionResult.NotModified
            if (!response.isSuccessful) return@withContext TariffPlansActionResult.Failed(PortalFailure.Server)

            val items = response.body()?.items ?: return@withContext TariffPlansActionResult.Failed(PortalFailure.Parse)
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
            TariffPlansActionResult.Success
        } catch (_: HttpException) {
            TariffPlansActionResult.Failed(PortalFailure.Server)
        } catch (_: IOException) {
            TariffPlansActionResult.Failed(PortalFailure.Network)
        }
    }

    override fun paymentsFlow(): Flow<Payments?> = localStorage.paymentsFlow()

    override suspend fun refreshPayments(): PaymentsActionResult = withContext(dispatcher) {
        fetchPayments(pg = null, append = false)
    }

    override suspend fun loadMorePayments(): PaymentsActionResult = withContext(dispatcher) {
        val cached = localStorage.paymentsFlow().first() ?: return@withContext PaymentsActionResult.Success
        if (!cached.hasMore) return@withContext PaymentsActionResult.Success
        fetchPayments(pg = cached.nextPageOffset ?: cached.items.size, append = true)
    }

    private suspend fun fetchPayments(
        pg: Int?,
        append: Boolean,
    ): PaymentsActionResult = try {
        val response = portalApi.fetchFees(pg)
        when {
            response.code() == HTTP_UNAUTHORIZED -> {
                clearSessionCaches()
                PaymentsActionResult.Expired
            }

            !response.isSuccessful -> PaymentsActionResult.Failed(PortalFailure.Server)

            else -> handleFeesResponse(response.body(), pg, append)
        }
    } catch (_: HttpException) {
        PaymentsActionResult.Failed(PortalFailure.Server)
    } catch (_: IOException) {
        PaymentsActionResult.Failed(PortalFailure.Network)
    }

    private suspend fun handleFeesResponse(
        page: FeesPageResponse?,
        requestedOffset: Int?,
        append: Boolean,
    ): PaymentsActionResult = when {
        page == null -> PaymentsActionResult.Failed(PortalFailure.Parse)

        page.isLoginPage -> {
            clearSessionCaches()
            PaymentsActionResult.Expired
        }

        page.error != null -> PaymentsActionResult.Failed(PortalFailure.Server)

        else -> saveFeesPage(page, requestedOffset, append)
    }

    private suspend fun saveFeesPage(
        page: FeesPageResponse,
        requestedOffset: Int?,
        append: Boolean,
    ): PaymentsActionResult {
        val catalog = localStorage.tariffPlansFlow().first()?.plans.orEmpty()
        val payments = page.items.mapNotNull { it.toDomain(catalog) }
        if (page.items.isNotEmpty() && payments.isEmpty()) {
            return PaymentsActionResult.Failed(PortalFailure.Parse)
        }

        val existing = if (append) localStorage.paymentsFlow().first()?.items.orEmpty() else emptyList()
        val merged = (existing + payments).distinct()
        // A page made entirely of rows we already have means the server repeated itself (or the
        // paginator pointed backwards). Stop here instead of looping forever.
        val repeatedPage = append && merged.size == existing.size
        // Paginator values describe raw table rows, so they stay valid even if a row is dropped
        // during mapping. Fall back to the previous size heuristic if absent.
        val nextOffsetBySize = (requestedOffset ?: 0) + page.items.size
        val hasMore = !repeatedPage && hasMoreAfter(page, nextOffsetBySize)
        localStorage.savePayments(
            Payments(
                items = merged,
                hasMore = hasMore,
                nextPageOffset = if (hasMore) page.nextPageOffset ?: nextOffsetBySize else null,
            ),
        )
        return PaymentsActionResult.Success
    }

    private fun hasMoreAfter(
        page: FeesPageResponse,
        nextOffsetBySize: Int,
    ): Boolean = page.totalCount?.let { total -> nextOffsetBySize < total }
        ?: (page.items.size == PAGE_SIZE)

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

    private suspend fun clearSessionCaches() {
        localStorage.clearProfile()
        sessionCookieStore.clear()
        localStorage.clearPayments()
    }

    private fun String.toPaymentSource(): PaymentSource = when {
        "abonplata_privatbank" in this -> PaymentSource.PRIVAT24
        "abonplata_easypay" in this -> PaymentSource.EASYPAY
        "abonplata_monobank" in this -> PaymentSource.MONOBANK
        else -> PaymentSource.UNKNOWN
    }

    private companion object {
        const val STATUS_ACTIVE = "Enable"
        const val HTTP_UNAUTHORIZED = 401
        const val PAGE_SIZE = 25
        val FEES_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
