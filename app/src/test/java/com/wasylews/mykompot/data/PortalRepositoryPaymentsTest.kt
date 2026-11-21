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
import com.wasylews.mykompot.domain.model.PaymentsActionResult
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response
import java.time.LocalDateTime

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
    fun `refreshPayments uses paginator total and next offset`() = runTest {
        val storage = FakeLocalStorage().apply { saveSession("test-session") }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(
                FeesPageResponse(
                    items = List(25) { feeItem(it) },
                    currentPage = "1",
                    pageOffsets = listOf("25", "50", "75", "100"),
                    total = "120",
                ),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        assertEquals(PaymentsActionResult.Success, repo.refreshPayments())

        repo.paymentsFlow().test {
            val payments = awaitItem()
            assertEquals(25, payments?.items?.size)
            assertEquals(true, payments?.hasMore)
            assertEquals(25, payments?.nextPageOffset)
        }
    }

    @Test
    fun `paginator total stops loading at the last page`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            savePayments(
                Payments(
                    items = List(100) { feeItem(it + 100).toPayment() },
                    hasMore = true,
                    nextPageOffset = 100,
                ),
            )
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(
                FeesPageResponse(
                    items = List(20) { feeItem(it) },
                    currentPage = "100",
                    pageOffsets = listOf("1", "25", "50", "75"),
                    total = "120",
                ),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        assertEquals(PaymentsActionResult.Success, repo.loadMorePayments())

        assertEquals(listOf<Int?>(100), api.feesPgRequests)
        repo.paymentsFlow().test {
            val payments = awaitItem()
            assertEquals(120, payments?.items?.size)
            assertEquals(false, payments?.hasMore)
            assertNull(payments?.nextPageOffset)
        }
    }

    @Test
    fun `loadMorePayments uses the stored paginator offset`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            savePayments(
                Payments(
                    items = listOf(feeItem(0).toPayment()),
                    hasMore = true,
                    nextPageOffset = 25,
                ),
            )
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(FeesPageResponse(items = listOf(feeItem(1)))),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        assertEquals(PaymentsActionResult.Success, repo.loadMorePayments())

        assertEquals(listOf<Int?>(25), api.feesPgRequests)
    }

    @Test
    fun `loadMorePayments does not duplicate rows repeated across pages`() = runTest {
        val repeated = feeItem(0).toPayment()
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            savePayments(Payments(items = listOf(repeated), hasMore = true, nextPageOffset = 25))
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(FeesPageResponse(items = listOf(feeItem(0), feeItem(1)))),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        assertEquals(PaymentsActionResult.Success, repo.loadMorePayments())

        repo.paymentsFlow().test {
            val payments = awaitItem()
            assertEquals(2, payments?.items?.size)
            assertEquals(repeated, payments?.items?.first())
        }
    }

    @Test
    fun `loadMorePayments stops when the server repeats a page`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            savePayments(
                Payments(items = listOf(feeItem(0).toPayment()), hasMore = true, nextPageOffset = 25),
            )
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.success(
                FeesPageResponse(
                    items = listOf(feeItem(0)),
                    currentPage = "25",
                    pageOffsets = listOf("1", "50"),
                    total = "100",
                ),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        assertEquals(PaymentsActionResult.Success, repo.loadMorePayments())

        repo.paymentsFlow().test {
            val payments = awaitItem()
            assertEquals(1, payments?.items?.size)
            assertEquals(false, payments?.hasMore)
            assertNull(payments?.nextPageOffset)
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
    fun `refreshPayments returns expired on 401`() = runTest {
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            saveProfile(profile())
            savePayments(Payments(items = emptyList(), hasMore = false))
        }
        val api = FakePortalApi(
            response = activeProfileResponse(),
            feesResponse = Response.error(
                401,
                "Unauthorized".toResponseBody("text/plain".toMediaType()),
            ),
        )
        val repo = createPortalRepository(testScheduler, portalApi = api, localStorage = storage)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Expired, result)
        assertNull(storage.sessionFlow().first())
        assertNull(storage.profileFlow().first())
        assertNull(storage.paymentsFlow().first())
    }

    @Test
    fun `refreshPayments requests fees even when no session is stored`() = runTest {
        val api = FakePortalApi(response = activeProfileResponse())
        val repo = createPortalRepository(testScheduler, portalApi = api)

        val result = repo.refreshPayments()

        assertEquals(PaymentsActionResult.Success, result)
        assertEquals(1, api.fetchFeesCalls)
    }
}
