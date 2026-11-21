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

import app.cash.turbine.test
import com.wasylews.mykompot.domain.model.Payment
import com.wasylews.mykompot.domain.model.PaymentSource
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import okhttp3.Headers
import org.junit.Assert.assertEquals
import retrofit2.Call
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal fun createPortalRepository(
    testScheduler: TestCoroutineScheduler,
    portalApi: FakePortalApi = FakePortalApi(activeProfileResponse()),
    localStorage: FakeLocalStorage = FakeLocalStorage(),
    sessionCookieStore: SessionCookieStore = LocalStorageSessionCookieStore(localStorage),
): PortalRepositoryImpl = PortalRepositoryImpl(
    dispatcher = StandardTestDispatcher(testScheduler),
    authApi = portalApi,
    portalApi = portalApi,
    plansApi = portalApi,
    localStorage = localStorage,
    sessionCookieStore = sessionCookieStore,
)

internal suspend fun refreshFixture(
    testScheduler: TestCoroutineScheduler,
    profile: Profile? = null,
    api: FakePortalApi = FakePortalApi(activeProfileResponse()),
): Pair<PortalRepositoryImpl, FakePortalApi> {
    val storage = FakeLocalStorage().apply {
        saveSession("test-session")
        if (profile != null) saveProfile(profile)
    }
    val repo = createPortalRepository(
        testScheduler = testScheduler,
        portalApi = api,
        localStorage = storage,
    )
    return repo to api
}

internal suspend fun PortalRepositoryImpl.expectProfile(expected: Profile?) {
    profileFlow().test {
        assertEquals(expected, awaitItem())
    }
}

internal suspend fun PortalRepositoryImpl.expectSession(expected: String?) {
    sessionFlow().test {
        assertEquals(expected, awaitItem())
    }
}

internal fun activeProfileResponse() = ProfilePageResponse(
    status = "Enable",
    plan = "[35] 300GRN_100mb",
    nextPayment = "Next periodic payments throught days (2026-11-01)",
)

internal fun inactiveProfileResponse() = ProfilePageResponse(
    status = "Too Small Deposit",
    plan = "[35] 300GRN_100mb",
    nextPayment = null,
)

internal fun loginPage() = ProfilePageResponse(
    loginFormAction = "https://kompot.uz.ua:9443/index.cgi",
)

internal fun genericError() = ProfilePageResponse(
    error = "Service temporarily unavailable",
)

internal fun profile() = Profile(
    isActive = true,
    nextPaymentDate = LocalDate.of(2026, 11, 1),
    tariffPlan = defaultTariffPlan(),
)

internal fun inactiveProfile() = Profile(
    isActive = false,
    nextPaymentDate = null,
    tariffPlan = defaultTariffPlan(),
)

/** Intentionally uses the raw id as the name so catalog-missing fallback assertions stay meaningful. */
internal fun defaultTariffPlan() = TariffPlan(
    id = "300GRN_100mb",
    name = "300GRN_100mb",
    speed = 100,
    price = 300,
)

internal fun plansPageResponse() = PlansPageResponse(
    items = listOf(
        ParsedPlanItem(name = "Econom 50", price = "260 грн./міс.", speed = 50),
        ParsedPlanItem(name = "Standart 100", price = "300 грн./міс.", speed = 100),
        ParsedPlanItem(name = "LUX 300", price = "350 грн./міс.", speed = 300),
        ParsedPlanItem(name = "Подобова оплата", price = "15 грн./доба", speed = 50),
    ),
)

internal fun feeItem(index: Int) = ParsedFeeItem(
    date = LocalDateTime.of(2026, 10, 1, 10, 0).minusDays(index.toLong())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
    description = "abonplata_privatbank",
    sum = "300",
)

internal fun ParsedFeeItem.toPayment() = Payment(
    dateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
    amount = sum.toInt(),
    tariffName = sum,
    source = PaymentSource.PRIVAT24,
)

internal class FakePortalApi(
    private val response: ProfilePageResponse,
    private val exception: Exception? = null,
    private val fetchException: Exception? = exception,
    private val fetchResponse: Response<ProfilePageResponse>? = null,
    private val loginResponse: Response<ProfilePageResponse>? = null,
    private val plansException: Exception? = null,
    private val plansResponse: Response<PlansPageResponse>? = null,
    private val feesException: Exception? = null,
    private val feesResponse: Response<FeesPageResponse>? = null,
    private val feesResponsesByPg: Map<Int?, Response<FeesPageResponse>> = emptyMap(),
) : AuthApi,
    PortalApi,
    PlansApi {
    var fetchCalls = 0
    var fetchPlansCalls = 0
    var fetchFeesCalls = 0
    var lastIfNoneMatch: String? = null
    var lastIfModifiedSince: String? = null
    var lastLoginLanguage: String? = null
    val feesPgRequests = mutableListOf<Int?>()

    override suspend fun login(
        username: String,
        password: String,
        language: String,
    ): Response<ProfilePageResponse> {
        lastLoginLanguage = language
        exception?.let { throw it }
        return loginResponse ?: Response.success(
            response,
            Headers.Builder()
                .add("Set-Cookie", "sid=test-session")
                .build(),
        )
    }

    override fun loginCall(
        username: String,
        password: String,
        language: String,
    ): Call<ProfilePageResponse> = FakeCall(response = {
        lastLoginLanguage = language
        exception?.let { throw it }
        loginResponse ?: Response.success(response)
    })

    override suspend fun fetchProfile(): Response<ProfilePageResponse> {
        fetchCalls++
        fetchException?.let { throw it }
        return fetchResponse ?: Response.success(response)
    }

    override suspend fun fetchPlans(
        ifNoneMatch: String?,
        ifModifiedSince: String?,
    ): Response<PlansPageResponse> {
        fetchPlansCalls++
        lastIfNoneMatch = ifNoneMatch
        lastIfModifiedSince = ifModifiedSince
        plansException?.let { throw it }
        return plansResponse ?: Response.success(PlansPageResponse(emptyList()))
    }

    override suspend fun fetchFees(pg: Int?): Response<FeesPageResponse> {
        fetchFeesCalls++
        feesPgRequests += pg
        feesException?.let { throw it }
        return feesResponsesByPg[pg]
            ?: feesResponse
            ?: Response.success(FeesPageResponse())
    }
}

/** Test adapter that keeps the repository seam backed by the existing fake storage. */
internal class LocalStorageSessionCookieStore(
    private val storage: FakeLocalStorage,
) : SessionCookieStore {
    override fun sid(): String? = runBlocking { storage.sessionFlow().first() }

    override fun saveSid(sid: String) = runBlocking { storage.saveSession(sid) }

    override fun clear() = runBlocking { storage.clearSession() }

    override fun sidFlow(): Flow<String?> = storage.sessionFlow()
}

internal class FakeLocalStorage : LocalStorage {
    private val savedProfile = MutableStateFlow<Profile?>(null)
    private val savedSession = MutableStateFlow<String?>(null)
    private val savedTariffPlans = MutableStateFlow<TariffPlans?>(null)
    private val savedPayments = MutableStateFlow<Payments?>(null)
    var saveTariffPlansCalls = 0
    var savePaymentsCalls = 0

    override suspend fun saveProfile(profile: Profile) {
        savedProfile.value = profile
    }

    override fun profileFlow(): Flow<Profile?> = savedProfile

    fun sessionFlow(): Flow<String?> = savedSession

    suspend fun saveSession(sid: String) {
        savedSession.value = sid
    }

    override suspend fun clearProfile() {
        savedProfile.value = null
    }

    suspend fun clearSession() {
        savedSession.value = null
    }

    override fun tariffPlansFlow(): Flow<TariffPlans?> = savedTariffPlans

    override suspend fun saveTariffPlans(tariffPlans: TariffPlans) {
        saveTariffPlansCalls++
        savedTariffPlans.value = tariffPlans
    }

    override fun paymentsFlow(): Flow<Payments?> = savedPayments

    override suspend fun savePayments(payments: Payments) {
        savePaymentsCalls++
        savedPayments.value = payments
    }

    override suspend fun clearPayments() {
        savedPayments.value = null
    }
}
