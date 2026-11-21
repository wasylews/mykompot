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
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import com.wasylews.mykompot.domain.model.TariffPlansActionResult
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class PortalRepositoryTariffPlansTest {
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
}
