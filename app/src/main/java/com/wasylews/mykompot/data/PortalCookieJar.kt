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

import com.wasylews.mykompot.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.annotation.Singleton

/**
 * Cookie jar for the portal. It keeps the `sid` cookie in [SessionCookieStore] and always sends
 * the language cookie so parsed English labels stay stable. Cookies are scoped to the portal host.
 */
@Singleton
class PortalCookieJar(
    private val store: SessionCookieStore,
) : CookieJar {
    private val portalHost = BuildConfig.BASE_URL.toHttpUrl().host

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (url.host != portalHost) return emptyList()
        return listOfNotNull(
            cookie(LANGUAGE_COOKIE, PORTAL_LANGUAGE, url.host),
            store.sid()?.let { cookie(SID_COOKIE, it, url.host) },
        )
    }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) {
        if (url.host != portalHost) return
        cookies.firstOrNull { it.name == SID_COOKIE }?.let { store.saveSid(it.value) }
    }

    private fun cookie(
        name: String,
        value: String,
        host: String,
    ): Cookie = Cookie.Builder()
        .name(name)
        .value(value)
        .domain(host)
        .path("/")
        .build()

    private companion object {
        const val LANGUAGE_COOKIE = "language"
        const val SID_COOKIE = "sid"
        const val PORTAL_LANGUAGE = "english"
    }
}
