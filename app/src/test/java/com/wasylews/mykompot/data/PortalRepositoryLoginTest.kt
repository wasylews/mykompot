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
import com.wasylews.mykompot.domain.model.PortalFailure
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate

class PortalRepositoryLoginTest {
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
    fun `login stores sid parsed from response html`() = runTest {
        val api = FakePortalApi(activeProfileResponse().copy(sid = "html-session"))
        val repo = createPortalRepository(testScheduler, api)

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.Success(profile()), result)
        repo.expectSession("html-session")
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
}
