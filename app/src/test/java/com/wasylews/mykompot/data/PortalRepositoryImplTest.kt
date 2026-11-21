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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalDateTime

class PortalRepositoryImplTest {
    @Test
    fun `login with wrong credentials`() = runTest {
        val repo = createPortalRepository(testScheduler, FakePortalApi(loginPage()))

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.InvalidCredentials, result)
    }

    @Test
    fun `login with a generic portal error is a server failure`() = runTest {
        val repo = createPortalRepository(testScheduler, FakePortalApi(genericError()))

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.Failed(PortalFailure.Server), result)
    }

    @Test
    fun `login with valid credentials`() = runTest {
        val api = FakePortalApi(activeProfileResponse())
        val repo = createPortalRepository(testScheduler, api)

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.Success(profile()), result)
        assertEquals("english", api.lastLoginLanguage)
    }

    @Test
    fun `network error`() = runTest {
        val portalApi = FakePortalApi(activeProfileResponse(), exception = IOException())
        val repo = createPortalRepository(testScheduler, portalApi)
        val result = repo.login("user", "pass")

        assertEquals(LoginResult.Failed(PortalFailure.Network), result)
    }

    @Test
    fun `server error`() = runTest {
        val response = Response.error<ProfilePageResponse>(
            500,
            "Server side issue".toResponseBody(
                "text/plain".toMediaType(),
            ),
        )
        val portalApi = FakePortalApi(activeProfileResponse(), exception = HttpException(response))
        val repo = createPortalRepository(testScheduler, portalApi)
        val result = repo.login("user", "pass")

        assertEquals(LoginResult.Failed(PortalFailure.Server), result)
    }

    @Test
    fun `profile flow emits the saved profile`() = runTest {
        val repo = createPortalRepository(testScheduler)
        repo.login("user", "pass")

        repo.expectProfile(profile())
    }

    @Test
    fun `login resolves tariff plan from catalog`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveTariffPlans(
                TariffPlans(
                    plans = listOf(
                        TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
                        TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
                    ),
                ),
            )
        }
        val repo = createPortalRepository(testScheduler, localStorage = storage)

        repo.login("user", "pass")

        repo.expectProfile(
            profile().copy(
                tariffPlan = TariffPlan(
                    id = "300GRN_100mb",
                    name = "Standart 100",
                    speed = 100,
                    price = 300,
                ),
            ),
        )
    }

    @Test
    fun `login derives tariff plan from id when catalog is missing`() = runTest {
        val repo = createPortalRepository(
            testScheduler,
            FakePortalApi(
                ProfilePageResponse(
                    status = "Enable",
                    plan = "[35] 260GRN_50mb",
                    nextPayment = "Next periodic payments throught days (2026-11-01)",
                ),
            ),
        )

        repo.login("user", "pass")

        repo.expectProfile(
            Profile(
                isActive = true,
                nextPaymentDate = LocalDate.of(2026, 11, 1),
                tariffPlan = TariffPlan(
                    id = "260GRN_50mb",
                    name = "260GRN_50mb",
                    speed = 50,
                    price = 260,
                ),
            ),
        )
    }

    @Test
    fun `session flow emits the stored session cookie`() = runTest {
        val storage = FakeLocalStorage().apply { saveSession("test-session") }
        val repo = createPortalRepository(testScheduler, localStorage = storage)

        repo.sessionFlow().test {
            assertEquals("test-session", awaitItem())
        }
    }

    @Test
    fun `refresh keeps cached profile on network error`() = runTest {
        val cachedProfile = profile()
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = cachedProfile,
            api = FakePortalApi(activeProfileResponse(), exception = IOException()),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Failed(PortalFailure.Network), result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(cachedProfile)
        repo.expectSession("test-session")
    }

    @Test
    fun `refresh saves fetched profile when cache is empty`() = runTest {
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            api = FakePortalApi(activeProfileResponse()),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Updated(profile()), result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(profile())
        repo.expectSession("test-session")
    }

    @Test
    fun `refresh saves inactive profile when next payment date is missing`() = runTest {
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            api = FakePortalApi(inactiveProfileResponse()),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Updated(inactiveProfile()), result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(inactiveProfile())
        repo.expectSession("test-session")
    }

    @Test
    fun `refresh clears session when server returns login page`() = runTest {
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = profile(),
            api = FakePortalApi(loginPage()),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Expired, result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(null)
        repo.expectSession(null)
    }

    @Test
    fun `refresh keeps caches on generic portal error`() = runTest {
        val cachedProfile = profile()
        val cachedPayments = Payments(items = emptyList(), hasMore = false)
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            saveProfile(cachedProfile)
            savePayments(cachedPayments)
        }
        val repo = createPortalRepository(
            testScheduler = testScheduler,
            portalApi = FakePortalApi(genericError()),
            localStorage = storage,
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Failed(PortalFailure.Server), result)
        repo.expectProfile(cachedProfile)
        repo.expectSession("test-session")
        assertEquals(cachedPayments, storage.paymentsFlow().first())
    }

    @Test
    fun `refresh returns server error on unsuccessful response`() = runTest {
        val cachedProfile = profile()
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = cachedProfile,
            api = FakePortalApi(
                response = activeProfileResponse(),
                fetchResponse = Response.error(
                    502,
                    "Bad Gateway".toResponseBody("text/plain".toMediaType()),
                ),
            ),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Failed(PortalFailure.Server), result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(cachedProfile)
        repo.expectSession("test-session")
    }

    @Test
    fun `refresh returns server error on http exception`() = runTest {
        val cachedProfile = profile()
        val errorResponse = Response.error<ProfilePageResponse>(
            500,
            "Server side issue".toResponseBody("text/plain".toMediaType()),
        )
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = cachedProfile,
            api = FakePortalApi(
                response = activeProfileResponse(),
                exception = HttpException(errorResponse),
            ),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Failed(PortalFailure.Server), result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(cachedProfile)
        repo.expectSession("test-session")
    }

    @Test
    fun `refresh returns parse error on null body`() = runTest {
        val cachedProfile = profile()
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = cachedProfile,
            api = FakePortalApi(
                response = activeProfileResponse(),
                fetchResponse = Response.success(null),
            ),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Failed(PortalFailure.Parse), result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(cachedProfile)
        repo.expectSession("test-session")
    }

    @Test
    fun `refresh returns parse error when profile cannot be parsed`() = runTest {
        val cachedProfile = profile()
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = cachedProfile,
            api = FakePortalApi(response = ProfilePageResponse(status = "Enable", plan = null)),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Failed(PortalFailure.Parse), result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(cachedProfile)
        repo.expectSession("test-session")
    }

    @Test
    fun `refresh returns expired when no session is stored`() = runTest {
        val api = FakePortalApi(activeProfileResponse())
        val repo = createPortalRepository(testScheduler, portalApi = api)

        val result = repo.refresh()

        assertEquals(RefreshResult.Expired, result)
        assertEquals(0, api.fetchCalls)
    }

    @Test
    fun `login returns parsing error when response body is missing`() = runTest {
        val repo = createPortalRepository(
            testScheduler,
            FakePortalApi(
                response = activeProfileResponse(),
                loginResponse = Response.success(null),
            ),
        )

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.Failed(PortalFailure.Parse), result)
    }

    @Test
    fun `tariff plans flow emits null when plans are missing`() = runTest {
        val repo = createPortalRepository(testScheduler)

        repo.tariffPlansFlow().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `refreshTariffPlans saves fetched monthly plans`() = runTest {
        val repo = createPortalRepository(
            testScheduler,
            portalApi = FakePortalApi(
                response = activeProfileResponse(),
                plansResponse = Response.success(plansPageResponse()),
            ),
        )

        repo.refreshTariffPlans()

        repo.tariffPlansFlow().test {
            assertEquals(
                TariffPlans(
                    plans = listOf(
                        TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
                        TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
                        TariffPlan(id = "350GRN_300mb", name = "LUX 300", speed = 300, price = 350),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `refreshTariffPlans fails when response is unsuccessful`() = runTest {
        val repo = createPortalRepository(
            testScheduler,
            portalApi = FakePortalApi(
                response = activeProfileResponse(),
                plansResponse = Response.error(
                    502,
                    "Bad Gateway".toResponseBody("text/plain".toMediaType()),
                ),
            ),
        )

        val result = repo.refreshTariffPlans()

        assertEquals(TariffPlansActionResult.Failed(PortalFailure.Server), result)
        repo.tariffPlansFlow().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `refreshTariffPlans fails when network is unavailable`() = runTest {
        val repo = createPortalRepository(
            testScheduler,
            portalApi = FakePortalApi(
                response = activeProfileResponse(),
                plansException = IOException(),
            ),
        )

        val result = repo.refreshTariffPlans()

        assertEquals(TariffPlansActionResult.Failed(PortalFailure.Network), result)
    }

    @Test
    fun `refreshTariffPlans fails when body is missing`() = runTest {
        val repo = createPortalRepository(
            testScheduler,
            portalApi = FakePortalApi(
                response = activeProfileResponse(),
                plansResponse = Response.success(null),
            ),
        )

        val result = repo.refreshTariffPlans()

        assertEquals(TariffPlansActionResult.Failed(PortalFailure.Parse), result)
        repo.tariffPlansFlow().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `refreshTariffPlans keeps cached plans when server returns not modified`() = runTest {
        val cachedPlans = listOf(
            TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
        )
        val storage = FakeLocalStorage().apply {
            saveTariffPlans(
                TariffPlans(
                    plans = cachedPlans,
                    etag = "\"etag\"",
                    lastModified = "Mon, 28 Sep 2026 10:14:15 GMT",
                ),
            )
            saveTariffPlansCalls = 0
        }
        val rawResponse = okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("http://localhost/plans/").build())
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(304)
            .message("Not Modified")
            .build()
        val api = FakePortalApi(
            response = activeProfileResponse(),
            plansResponse = Response.error(
                "".toResponseBody("text/plain".toMediaType()),
                rawResponse,
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        repo.refreshTariffPlans()

        assertEquals(1, api.fetchPlansCalls)
        assertEquals("\"etag\"", api.lastIfNoneMatch)
        assertEquals("Mon, 28 Sep 2026 10:14:15 GMT", api.lastIfModifiedSince)
        assertEquals(0, storage.saveTariffPlansCalls)
        repo.tariffPlansFlow().test {
            assertEquals(
                TariffPlans(
                    plans = cachedPlans,
                    etag = "\"etag\"",
                    lastModified = "Mon, 28 Sep 2026 10:14:15 GMT",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `refreshTariffPlans saves plans and validators when response changes`() = runTest {
        val storage = FakeLocalStorage()
        val rawResponse = okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("http://localhost/plans/").build())
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("ETag", "\"new-etag\"")
            .header("Last-Modified", "Tue, 29 Sep 2026 10:14:15 GMT")
            .build()
        val api = FakePortalApi(
            response = activeProfileResponse(),
            plansResponse = Response.success(plansPageResponse(), rawResponse),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        repo.refreshTariffPlans()

        assertEquals(1, api.fetchPlansCalls)
        storage.tariffPlansFlow().test {
            assertEquals(
                TariffPlans(
                    plans = listOf(
                        TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
                        TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
                        TariffPlan(id = "350GRN_300mb", name = "LUX 300", speed = 300, price = 350),
                    ),
                    etag = "\"new-etag\"",
                    lastModified = "Tue, 29 Sep 2026 10:14:15 GMT",
                ),
                awaitItem(),
            )
        }
    }

    private fun createPortalRepository(
        testScheduler: TestCoroutineScheduler,
        portalApi: FakePortalApi = FakePortalApi(activeProfileResponse()),
        localStorage: LocalStorage = FakeLocalStorage(),
        sessionCookieStore: SessionCookieStore = LocalStorageSessionCookieStore(localStorage),
    ): PortalRepositoryImpl = PortalRepositoryImpl(
        dispatcher = StandardTestDispatcher(testScheduler),
        authApi = portalApi,
        portalApi = portalApi,
        plansApi = portalApi,
        localStorage = localStorage,
        sessionCookieStore = sessionCookieStore,
    )

    private suspend fun refreshFixture(
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

    private suspend fun PortalRepositoryImpl.expectProfile(expected: Profile?) {
        profileFlow().test {
            assertEquals(expected, awaitItem())
        }
    }

    private suspend fun PortalRepositoryImpl.expectSession(expected: String?) {
        sessionFlow().test {
            assertEquals(expected, awaitItem())
        }
    }

    private fun activeProfileResponse() = ProfilePageResponse(
        status = "Enable",
        plan = "[35] 300GRN_100mb",
        nextPayment = "Next periodic payments throught days (2026-11-01)",
    )

    private fun inactiveProfileResponse() = ProfilePageResponse(
        status = "Too Small Deposit",
        plan = "[35] 300GRN_100mb",
        nextPayment = null,
    )

    private fun loginPage() = ProfilePageResponse(
        loginFormAction = "https://kompot.uz.ua:9443/index.cgi",
    )

    private fun genericError() = ProfilePageResponse(
        error = "Service temporarily unavailable",
    )

    private fun profile() = Profile(
        isActive = true,
        nextPaymentDate = LocalDate.of(2026, 11, 1),
        tariffPlan = defaultTariffPlan(),
    )

    private fun inactiveProfile() = Profile(
        isActive = false,
        nextPaymentDate = null,
        tariffPlan = defaultTariffPlan(),
    )

    private fun defaultTariffPlan() = TariffPlan(
        id = "300GRN_100mb",
        name = "300GRN_100mb",
        speed = 100,
        price = 300,
    )

    private fun plansPageResponse() = PlansPageResponse(
        items = listOf(
            ParsedPlanItem(name = "Econom 50", price = "260 грн./міс.", speed = 50),
            ParsedPlanItem(name = "Standart 100", price = "300 грн./міс.", speed = 100),
            ParsedPlanItem(name = "LUX 300", price = "350 грн./міс.", speed = 300),
            ParsedPlanItem(name = "Подобова оплата", price = "15 грн./доба", speed = 50),
        ),
    )
}

class PortalRepositoryPaymentsTest {
    @Test
    fun `payments flow emits null when payments are missing`() = runTest {
        val repo = createPortalRepository(testScheduler)

        repo.paymentsFlow().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `refreshPayments fetches first page and saves mapped payments`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            saveTariffPlans(
                TariffPlans(
                    plans = listOf(
                        TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
                    ),
                ),
            )
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(
                FeesPageResponse(
                    items = listOf(
                        ParsedFeeItem(
                            date = "2026-10-01 10:05:08",
                            description = "abonplata_privatbank",
                            sum = "300",
                        ),
                        ParsedFeeItem(
                            date = "2024-10-15 09:39:29",
                            description = "abonplata_easypay",
                            sum = "250",
                        ),
                    ),
                ),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Success, result)
        assertEquals(1, api.fetchFeesCalls)
        assertEquals(listOf<Int?>(null), api.feesPgRequests)
        repo.paymentsFlow().test {
            assertEquals(
                Payments(
                    items = listOf(
                        Payment(
                            dateTime = LocalDateTime.of(2026, 10, 1, 10, 5, 8),
                            amount = 300,
                            tariffName = "Standart 100",
                            source = PaymentSource.PRIVAT24,
                        ),
                        Payment(
                            dateTime = LocalDateTime.of(2024, 10, 15, 9, 39, 29),
                            amount = 250,
                            tariffName = "250",
                            source = PaymentSource.EASYPAY,
                        ),
                    ),
                    hasMore = false,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `refreshPayments marks hasMore when page is full`() = runTest {
        val storage = FakeLocalStorage().apply { saveSession("test-session") }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(
                FeesPageResponse(items = List(25) { feeItem(index = it) }),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Success, result)
        repo.paymentsFlow().test {
            val payments = awaitItem()
            assertEquals(25, payments?.items?.size)
            assertEquals(true, payments?.hasMore)
        }
    }

    @Test
    fun `loadMorePayments appends next page using item count offset`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            savePayments(
                Payments(
                    items = List(25) { index ->
                        Payment(
                            dateTime = LocalDateTime.of(2026, 10, 1, 10, 0).minusDays(index.toLong()),
                            amount = 300,
                            tariffName = "300",
                            source = PaymentSource.PRIVAT24,
                        )
                    },
                    hasMore = true,
                ),
            )
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(
                FeesPageResponse(
                    items = listOf(
                        ParsedFeeItem(
                            date = "2024-10-15 09:39:29",
                            description = "abonplata_easypay",
                            sum = "250",
                        ),
                    ),
                ),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        val result = repo.loadMorePayments()

        assertEquals(PaymentsActionResult.Success, result)
        assertEquals(1, api.fetchFeesCalls)
        assertEquals(listOf<Int?>(25), api.feesPgRequests)
        repo.paymentsFlow().test {
            val payments = awaitItem()
            assertEquals(26, payments?.items?.size)
            assertEquals(
                Payment(
                    dateTime = LocalDateTime.of(2024, 10, 15, 9, 39, 29),
                    amount = 250,
                    tariffName = "250",
                    source = PaymentSource.EASYPAY,
                ),
                payments?.items?.last(),
            )
            assertEquals(false, payments?.hasMore)
        }
    }

    @Test
    fun `refreshPayments replaces cached pages`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            savePayments(
                Payments(
                    items = listOf(
                        Payment(
                            dateTime = LocalDateTime.of(2024, 1, 1, 0, 0),
                            amount = 100,
                            tariffName = "old",
                            source = PaymentSource.UNKNOWN,
                        ),
                    ),
                    hasMore = true,
                ),
            )
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(
                FeesPageResponse(
                    items = listOf(
                        ParsedFeeItem(
                            date = "2026-10-01 10:05:08",
                            description = "abonplata_monobank",
                            sum = "300",
                        ),
                    ),
                ),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Success, result)
        assertEquals(listOf<Int?>(null), api.feesPgRequests)
        repo.paymentsFlow().test {
            assertEquals(
                Payments(
                    items = listOf(
                        Payment(
                            dateTime = LocalDateTime.of(2026, 10, 1, 10, 5, 8),
                            amount = 300,
                            tariffName = "300",
                            source = PaymentSource.MONOBANK,
                        ),
                    ),
                    hasMore = false,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `refreshPayments returns network error and keeps cache`() = runTest {
        val cached = Payments(items = emptyList(), hasMore = true)
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            savePayments(cached)
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesException = IOException(),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Failed(PortalFailure.Network), result)
        repo.paymentsFlow().test {
            assertEquals(cached, awaitItem())
        }
    }

    @Test
    fun `refreshPayments returns server error and keeps cache on portal error`() = runTest {
        val cached = Payments(items = emptyList(), hasMore = false)
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            saveProfile(profile())
            savePayments(cached)
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(
                FeesPageResponse(error = "Service temporarily unavailable"),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Failed(PortalFailure.Server), result)
        assertEquals("test-session", storage.sessionFlow().first())
        assertEquals(profile(), storage.profileFlow().first())
        assertEquals(cached, storage.paymentsFlow().first())
    }

    @Test
    fun `refreshPayments returns expired and clears session when login page is served`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            saveProfile(profile())
            savePayments(Payments(items = emptyList(), hasMore = false))
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(FeesPageResponse(loginFormAction = "https://kompot.uz.ua:9443/index.cgi")),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Expired, result)
        assertEquals(null, storage.sessionFlow().first())
        assertEquals(null, storage.profileFlow().first())
        assertEquals(null, storage.paymentsFlow().first())
    }

    @Test
    fun `refreshPayments returns expired when session is missing`() = runTest {
        val api = FakePortalApi(response = activeProfileResponse())
        val repo = createPortalRepository(testScheduler, portalApi = api)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Expired, result)
        assertEquals(0, api.fetchFeesCalls)
    }

    private fun createPortalRepository(
        testScheduler: TestCoroutineScheduler,
        portalApi: FakePortalApi = FakePortalApi(activeProfileResponse()),
        localStorage: LocalStorage = FakeLocalStorage(),
        sessionCookieStore: SessionCookieStore = LocalStorageSessionCookieStore(localStorage),
    ): PortalRepositoryImpl = PortalRepositoryImpl(
        dispatcher = StandardTestDispatcher(testScheduler),
        authApi = portalApi,
        portalApi = portalApi,
        plansApi = portalApi,
        localStorage = localStorage,
        sessionCookieStore = sessionCookieStore,
    )

    private fun activeProfileResponse() = ProfilePageResponse(
        status = "Enable",
        plan = "[35] 300GRN_100mb",
        nextPayment = "Next periodic payments throught days (2026-11-01)",
    )

    private fun profile() = Profile(
        isActive = true,
        nextPaymentDate = LocalDate.of(2026, 11, 1),
        tariffPlan = TariffPlan(
            id = "300GRN_100mb",
            name = "300GRN_100mb",
            speed = 100,
            price = 300,
        ),
    )

    private fun feeItem(index: Int) = ParsedFeeItem(
        date = LocalDateTime.of(2026, 10, 1, 10, 0).minusDays(index.toLong())
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        description = "abonplata_privatbank",
        sum = "300",
    )
}

private class FakePortalApi(
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
private class LocalStorageSessionCookieStore(
    private val storage: LocalStorage,
) : SessionCookieStore {
    override fun sid(): String? = runBlocking { storage.sessionFlow().first() }

    override fun saveSid(sid: String) = runBlocking { storage.saveSession(sid) }

    override fun clear() = runBlocking { storage.clearSession() }

    override fun sidFlow(): Flow<String?> = storage.sessionFlow()
}

private class FakeLocalStorage : LocalStorage {
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

    override fun sessionFlow(): Flow<String?> = savedSession

    override suspend fun saveSession(sid: String) {
        savedSession.value = sid
    }

    override suspend fun clearProfile() {
        savedProfile.value = null
    }

    override suspend fun clearSession() {
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
