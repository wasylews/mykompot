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

package com.wasylews.mykompot.di

import okhttp3.CookieJar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class DataModuleOkHttpTest {
    @Test
    fun `all clients share explicit bounded timeouts`() {
        val client = okHttpClient(CookieJar.NO_COOKIES)

        assertEquals(TimeUnit.SECONDS.toMillis(15), client.connectTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(20), client.readTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(20), client.writeTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(30), client.callTimeoutMillis.toLong())
    }

    @Test
    fun `clients keep the supplied cookie jar and standard redirect policy`() {
        val client = okHttpClient(CookieJar.NO_COOKIES)

        assertEquals(CookieJar.NO_COOKIES, client.cookieJar)
        assertEquals(true, client.followRedirects)
        assertEquals(true, client.followSslRedirects)
        assertEquals(true, client.retryOnConnectionFailure)
    }
}
