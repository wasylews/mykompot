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

import com.wasylews.mykompot.domain.FakeCredentialsStore
import com.wasylews.mykompot.domain.model.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import retrofit2.Response as RetrofitResponse

class PortalAuthenticatorTest {
    private val credentials = Credentials("user", "pass")

    @Test
    fun `successful re-login retries the original request once`() {
        // GIVEN
        val api = FakeAuthApi(
            response = RetrofitResponse.success(profilePage()),
            onLogin = { cookieStore.saveSid("new-session") },
        )
        val authenticator = authenticator(api)

        // WHEN
        val retry = authenticator.authenticate(null, unauthorized())

        // THEN
        assertNotNull(retry)
        assertNotNull(retry!!.tag(PortalReauthAttempt::class.java))
        assertEquals(1, api.loginCalls)
        assertEquals("user", api.lastUsername)
        assertEquals("pass", api.lastPassword)
        assertEquals("english", api.lastLanguage)
    }

    @Test
    fun `already retried request is not authenticated again`() {
        // GIVEN
        val api = FakeAuthApi(RetrofitResponse.success(profilePage()))
        val authenticator = authenticator(api)

        // WHEN
        val retry = authenticator.authenticate(null, unauthorized(tagged = true))

        // THEN
        assertNull(retry)
        assertEquals(0, api.loginCalls)
    }

    @Test
    fun `missing credentials do not trigger login`() {
        // GIVEN
        val api = FakeAuthApi(RetrofitResponse.success(profilePage()))
        val store = FakeCredentialsStore()
        val authenticator = PortalAuthenticator(api, store, cookieStore)

        // WHEN
        val retry = authenticator.authenticate(null, unauthorized())

        // THEN
        assertNull(retry)
        assertEquals(0, api.loginCalls)
    }

    @Test
    fun `invalid credentials are cleared and session is dropped`() {
        // GIVEN
        val api = FakeAuthApi(RetrofitResponse.success(ProfilePageResponse(loginFormAction = "/index.cgi")))
        val store = FakeCredentialsStore().apply { save(credentials) }
        val authenticator = PortalAuthenticator(api, store, cookieStore)

        // WHEN
        val retry = authenticator.authenticate(null, unauthorized())

        // THEN
        assertNull(retry)
        assertNull(store.read())
        assertNull(cookieStore.sid())
    }

    @Test
    fun `network failure keeps credentials`() {
        // GIVEN
        val api = FakeAuthApi(RetrofitResponse.success(profilePage()), exception = IOException())
        val store = FakeCredentialsStore().apply { save(credentials) }
        val authenticator = PortalAuthenticator(api, store, cookieStore)

        // WHEN
        val retry = authenticator.authenticate(null, unauthorized())

        // THEN
        assertNull(retry)
        assertEquals(credentials, store.read())
    }

    @Test
    fun `concurrent expired requests trigger one login`() {
        // GIVEN
        val enteredLogin = CountDownLatch(1)
        val releaseLogin = CountDownLatch(1)
        val api = FakeAuthApi(
            response = RetrofitResponse.success(profilePage()),
            onLogin = {
                cookieStore.saveSid("new-session")
                enteredLogin.countDown()
                releaseLogin.await(5, TimeUnit.SECONDS)
            },
        )
        val authenticator = authenticator(api)
        val executor = Executors.newFixedThreadPool(2)

        try {
            // WHEN
            val first = executor.submit(Callable { authenticator.authenticate(null, unauthorized()) })
            assertTrue(enteredLogin.await(5, TimeUnit.SECONDS))
            val second = executor.submit(Callable { authenticator.authenticate(null, unauthorized()) })
            releaseLogin.countDown()

            // THEN
            assertNotNull(first.get(5, TimeUnit.SECONDS))
            assertNotNull(second.get(5, TimeUnit.SECONDS))
            assertEquals(1, api.loginCalls)
        } finally {
            releaseLogin.countDown()
            executor.shutdownNow()
        }
    }

    private val cookieStore = FakeSessionCookieStore("old-session")

    private fun authenticator(api: FakeAuthApi): PortalAuthenticator = PortalAuthenticator(
        authApi = api,
        credentialsStore = FakeCredentialsStore().apply { save(credentials) },
        sessionCookieStore = cookieStore,
    )

    private fun unauthorized(
        sid: String = "old-session",
        tagged: Boolean = false,
    ): Response {
        var request = Request.Builder()
            .url("http://127.0.0.1:8080/index.cgi?index=43")
            .build()
            .withPortalSessionAttempt(sid)
        if (tagged) {
            request = request.withPortalReauthAttempt()
        }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/html".toMediaType()))
            .build()
    }

    private fun profilePage() = ProfilePageResponse(
        status = "State: Enable",
        plan = "[35] 300GRN_100mb",
    )

    private class FakeAuthApi(
        private val response: RetrofitResponse<ProfilePageResponse>,
        private val exception: Exception? = null,
        private val onLogin: () -> Unit = {},
    ) : AuthApi {
        var loginCalls = 0
        var lastUsername: String? = null
        var lastPassword: String? = null
        var lastLanguage: String? = null

        override suspend fun login(
            username: String,
            password: String,
            language: String,
        ): RetrofitResponse<ProfilePageResponse> = error("Suspend login is not used by the authenticator")

        override fun loginCall(
            username: String,
            password: String,
            language: String,
        ): Call<ProfilePageResponse> {
            loginCalls++
            lastUsername = username
            lastPassword = password
            lastLanguage = language
            onLogin()
            return FakeCall({ response }, exception)
        }
    }
}
