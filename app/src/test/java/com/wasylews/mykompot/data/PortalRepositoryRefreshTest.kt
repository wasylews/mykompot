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
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PortalRepositoryRefreshTest {
    @Test
    fun `session flow emits the stored session cookie`() = runTest {
        val storage = FakeLocalStorage().apply { saveSession("test-session") }
        val repo = createPortalRepository(testScheduler, localStorage = storage)

        repo.sessionFlow().test {
            assertEquals("test-session", awaitItem())
        }
    }

    @Test
    fun `logout clears session caches but keeps tariff plans`() = runTest {
        val cachedPayments = Payments(items = emptyList(), hasMore = false)
        val cachedPlans = TariffPlans(
            plans = listOf(TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300)),
        )
        val storage = FakeLocalStorage().apply {
            saveSession("test-session")
            saveProfile(profile())
            savePayments(cachedPayments)
            saveTariffPlans(cachedPlans)
        }
        val repo = createPortalRepository(testScheduler, localStorage = storage)

        repo.logOut()

        repo.expectProfile(null)
        repo.expectSession(null)
        assertNull(storage.paymentsFlow().first())
        assertEquals(cachedPlans, storage.tariffPlansFlow().first())
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
    fun `refresh returns expired on 401`() = runTest {
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = profile(),
            api = FakePortalApi(
                response = activeProfileResponse(),
                fetchResponse = Response.error(
                    401,
                    "Unauthorized".toResponseBody("text/plain".toMediaType()),
                ),
            ),
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
    fun `refresh requests the profile even when no session is stored`() = runTest {
        val api = FakePortalApi(activeProfileResponse())
        val repo = createPortalRepository(testScheduler, portalApi = api)

        val result = repo.refresh()

        assertTrue(result is RefreshResult.Updated)
        assertEquals(1, api.fetchCalls)
    }
}
