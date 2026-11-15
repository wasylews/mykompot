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
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.TariffPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate

class PortalRepositoryImplTest {
    @Test
    fun `login with wrong credentials`() = runTest {
        val repo = createPortalRepository(testScheduler, FakePortalApi(failedLogin()))

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.InvalidCredentials, result)
    }

    @Test
    fun `login with valid credentials`() = runTest {
        val repo = createPortalRepository(testScheduler, FakePortalApi(activeProfileResponse()))

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.Success, result)
    }

    @Test
    fun `network error`() = runTest {
        val portalApi = FakePortalApi(activeProfileResponse(), exception = IOException())
        val repo = createPortalRepository(testScheduler, portalApi)
        val result = repo.login("user", "pass")

        assertEquals(LoginResult.NetworkError, result)
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

        assertEquals(LoginResult.ServerError, result)
    }

    @Test
    fun `profile flow emits the saved profile`() = runTest {
        val repo = createPortalRepository(testScheduler)
        repo.login("user", "pass")

        repo.expectProfile(profile())
    }

    @Test
    fun `session flow emits the saved session`() = runTest {
        val repo = createPortalRepository(testScheduler)
        repo.login("user", "pass")

        repo.sessionFlow().test {
            val session = awaitItem()
            assertFalse(session.isNullOrEmpty())
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

        assertEquals(RefreshResult.Failed.Network, result)
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

        assertEquals(RefreshResult.Updated, result)
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

        assertEquals(RefreshResult.Updated, result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(inactiveProfile())
        repo.expectSession("test-session")
    }

    @Test
    fun `refresh persists rotated sid when server returns it`() = runTest {
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = profile(),
            api = FakePortalApi(
                activeProfileResponse(),
                fetchResponse = Response.success(
                    activeProfileResponse(),
                    Headers.Builder()
                        .add("Set-Cookie", "sid=rotated-session")
                        .build(),
                ),
            ),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Updated, result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(profile())
        repo.expectSession("rotated-session")
    }

    @Test
    fun `refresh clears session when server returns login page`() = runTest {
        val (repo, api) = refreshFixture(
            testScheduler = testScheduler,
            profile = profile(),
            api = FakePortalApi(failedLogin()),
        )

        val result = repo.refresh()

        assertEquals(RefreshResult.Expired, result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(null)
        repo.expectSession(null)
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

        assertEquals(RefreshResult.Failed.Server, result)
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

        assertEquals(RefreshResult.Failed.Server, result)
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

        assertEquals(RefreshResult.Failed.Parse, result)
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

        assertEquals(RefreshResult.Failed.Parse, result)
        assertEquals(1, api.fetchCalls)
        repo.expectProfile(cachedProfile)
        repo.expectSession("test-session")
    }

    @Test
    fun `tariff plans flow emits null when plans are missing`() = runTest {
        val repo = createPortalRepository(testScheduler)

        repo.tariffPlansFlow().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `refreshTariffPlans saves fetched plans`() = runTest {
        val repo = createPortalRepository(
            testScheduler,
            portalApi = FakePortalApi(
                response = activeProfileResponse(),
                plansResponse = plansPageResponse(),
            ),
        )

        repo.refreshTariffPlans()

        repo.tariffPlansFlow().test {
            assertEquals(
                listOf(
                    TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
                    TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
                    TariffPlan(id = "350GRN_300mb", name = "LUX 300", speed = 300, price = 350),
                ),
                awaitItem(),
            )
        }
    }

    private fun createPortalRepository(
        testScheduler: TestCoroutineScheduler,
        portalApi: PortalApi = FakePortalApi(activeProfileResponse()),
        localStorage: LocalStorage = FakeLocalStorage(),
    ): PortalRepositoryImpl = PortalRepositoryImpl(
        dispatcher = StandardTestDispatcher(testScheduler),
        api = portalApi,
        localStorage = localStorage,
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
        plan = "300GRN_100mb",
        nextPayment = "Next periodic payments throught days (2026-11-01)",
    )

    private fun inactiveProfileResponse() = ProfilePageResponse(
        status = "Too Small Deposit",
        plan = "300GRN_100mb",
        nextPayment = null,
    )

    private fun failedLogin() = ProfilePageResponse(
        error = "Wrong password",
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

private class FakePortalApi(
    private val response: ProfilePageResponse,
    private val exception: Exception? = null,
    private val fetchResponse: Response<ProfilePageResponse>? = null,
    private val plansResponse: PlansPageResponse? = null,
) : PortalApi {
    var fetchCalls = 0
    var fetchPlansCalls = 0

    override suspend fun login(
        username: String,
        password: String,
    ): Response<ProfilePageResponse> {
        exception?.let { throw it }
        return Response.success(
            response,
            Headers.Builder()
                .add("Set-Cookie", "sid=test-session")
                .build(),
        )
    }

    override suspend fun fetchProfile(sid: String): Response<ProfilePageResponse> {
        fetchCalls++
        exception?.let { throw it }
        return fetchResponse ?: Response.success(response)
    }

    override suspend fun fetchPlans(): Response<PlansPageResponse> {
        fetchPlansCalls++
        exception?.let { throw it }
        return Response.success(plansResponse ?: PlansPageResponse(emptyList()))
    }
}

private class FakeLocalStorage : LocalStorage {
    private val savedProfile = MutableStateFlow<Profile?>(null)
    private val savedSession = MutableStateFlow<String?>(null)
    private val savedTariffPlans = MutableStateFlow<List<TariffPlan>?>(null)

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

    override fun tariffPlansFlow(): Flow<List<TariffPlan>?> = savedTariffPlans

    override suspend fun saveTariffPlans(plans: List<TariffPlan>) {
        savedTariffPlans.value = plans
    }
}
