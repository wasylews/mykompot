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

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class PortalSessionTagInterceptorTest {
    @Test
    fun `captures the current sid on the outgoing request`() {
        val store = FakeSessionCookieStore("session-42")
        val client = OkHttpClient.Builder()
            .addInterceptor(PortalSessionTagInterceptor(store))
            .addInterceptor { chain: Interceptor.Chain ->
                assertEquals("session-42", chain.request().tag(PortalSessionAttempt::class.java)?.sid)
                response(chain.request())
            }
            .build()

        client.newCall(Request.Builder().url("http://127.0.0.1:8080/index.cgi").build()).execute()
    }

    private fun response(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("".toResponseBody("text/html".toMediaType()))
        .build()
}
