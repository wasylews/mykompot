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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalCookieJarTest {
    private val store = FakeSessionCookieStore()
    private val jar = PortalCookieJar(store)
    private val portalUrl = "http://127.0.0.1:8080/index.cgi".toHttpUrl()

    @Test
    fun `sends language cookie before login`() {
        val cookies = jar.loadForRequest(portalUrl)

        assertEquals(1, cookies.size)
        assertEquals("language", cookies.single().name)
        assertEquals("english", cookies.single().value)
    }

    @Test
    fun `stores sid cookie from portal response`() {
        jar.saveFromResponse(portalUrl, listOf(cookie("sid", "new-session")))

        assertEquals("new-session", store.sid())
        assertTrue(jar.loadForRequest(portalUrl).any { it.name == "sid" && it.value == "new-session" })
    }

    @Test
    fun `ignores other cookies from portal response`() {
        jar.saveFromResponse(portalUrl, listOf(cookie("language", "french")))

        assertEquals(null, store.sid())
    }

    @Test
    fun `does not send session cookies to another host`() {
        store.saveSid("test-session")

        assertTrue(jar.loadForRequest("http://example.com/index.cgi".toHttpUrl()).isEmpty())
    }

    private fun cookie(
        name: String,
        value: String,
    ): Cookie = Cookie.Builder()
        .name(name)
        .value(value)
        .domain(portalUrl.host)
        .path("/")
        .build()
}

private class FakeSessionCookieStore : SessionCookieStore {
    private val sid = MutableStateFlow<String?>(null)

    override fun sid(): String? = sid.value

    override fun saveSid(sid: String) {
        this.sid.value = sid
    }

    override fun clear() {
        sid.value = null
    }

    override fun sidFlow(): Flow<String?> = sid
}
