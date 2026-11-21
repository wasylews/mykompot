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
import okhttp3.Request
import okhttp3.Response
import org.koin.core.annotation.Singleton

/** Session snapshot attached to a request before OkHttp's bridge layer adds the Cookie header. */
internal data class PortalSessionAttempt(
    val sid: String?,
)

internal fun Request.withPortalSessionAttempt(sid: String?): Request = newBuilder()
    .tag(PortalSessionAttempt::class.java, PortalSessionAttempt(sid))
    .build()

/**
 * Captures the sid used for each portal request.
 *
 * OkHttp rewrites `response.request` before the authenticator sees it, dropping the Cookie header.
 * This application interceptor keeps the sid on the request as a tag so the authenticator can tell
 * whether another concurrent request already rotated the session.
 */
@Singleton
class PortalSessionTagInterceptor(
    private val sessionCookieStore: SessionCookieStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().withPortalSessionAttempt(sessionCookieStore.sid()),
    )
}
