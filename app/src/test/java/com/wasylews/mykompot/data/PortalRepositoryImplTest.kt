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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PortalRepositoryImplTest {
    @Test
    fun `login with wrong credentials`() = runTest {
        val repo = createPortalRepository(testScheduler, FakePortalApi(failedLogin()))

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.InvalidCredentials, result)
    }

    @Test
    fun `login with valid credentials`() = runTest {
        val repo = createPortalRepository(testScheduler, FakePortalApi(activeProfile()))

        val result = repo.login("user", "pass")

        assertEquals(LoginResult.Success, result)
    }

    @Test
    fun `network error`() = runTest {
        val portalApi = FakePortalApi(activeProfile(), exception = IOException())
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
        val portalApi = FakePortalApi(activeProfile(), exception = HttpException(response))
        val repo = createPortalRepository(testScheduler, portalApi)
        val result = repo.login("user", "pass")

        assertEquals(LoginResult.ServerError, result)
    }

    @Test
    fun `load saved profile`() = runTest {
        val repo = createPortalRepository(testScheduler)
        repo.login("user", "pass")

        val profile = repo.loadProfile()
        assertTrue(profile.isActive)
        assertEquals("300GRN_100mb", profile.tariffPlan.name)
    }

    private fun createPortalRepository(
        testScheduler: TestCoroutineScheduler,
        portalApi: PortalApi = FakePortalApi(activeProfile()),
    ): PortalRepositoryImpl = PortalRepositoryImpl(
        dispatcher = StandardTestDispatcher(testScheduler),
        api = portalApi,
        localStorage = FakeLocalStorage(),
    )

    private fun activeProfile() = ProfilePageResponse(
        status = "Enable",
        plan = "300GRN_100mb",
        nextPayment = "Next periodic payments throught days (2026-11-01)",
    )

    private fun failedLogin() = ProfilePageResponse(
        error = "Wrong password",
    )
}

private class FakePortalApi(
    private val response: ProfilePageResponse,
    private val exception: Exception? = null,
) : PortalApi {
    override suspend fun login(
        username: String,
        password: String,
    ) = response.also { exception?.let { throw it } }
}

private class FakeLocalStorage : LocalStorage {
    private var savedProfile: Profile? = null

    override suspend fun saveProfile(profile: Profile) {
        savedProfile = profile
    }

    override suspend fun loadProfile(): Profile = savedProfile ?: Profile.DEFAULT
}
