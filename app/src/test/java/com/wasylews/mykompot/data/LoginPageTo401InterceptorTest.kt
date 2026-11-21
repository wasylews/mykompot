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

import com.wasylews.mykompot.HtmlFixtures
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginPageTo401InterceptorTest {
    @Test
    fun `converts a login page 200 into 401`() {
        val response = execute(HtmlFixtures.loginFailed())

        assertEquals(401, response.code)
        assertEquals("Unauthorized", response.message)
    }

    @Test
    fun `keeps the response body readable`() {
        val response = execute(HtmlFixtures.loginFailed())

        assertTrue(response.body.string().contains("form_login"))
    }

    @Test
    fun `leaves a profile response untouched`() {
        val response = execute(HtmlFixtures.profile())

        assertEquals(200, response.code)
        assertTrue(response.body.string().contains("State:</strong> Enable"))
    }

    @Test
    fun `leaves a generic portal error untouched`() {
        val response = execute(HtmlFixtures.portalError())

        assertEquals(200, response.code)
        assertTrue(response.body.string().contains("err_message"))
    }

    private fun execute(body: String): Response = client(body)
        .newCall(Request.Builder().url("http://127.0.0.1:8080/index.cgi?index=43").build())
        .execute()

    private fun client(body: String): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(LoginPageTo401Interceptor())
        .addInterceptor { chain: Interceptor.Chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("text/html; charset=UTF-8".toMediaType()))
                .build()
        }
        .build()
}
