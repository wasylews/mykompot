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

import com.wasylews.mykompot.domain.repository.CredentialsStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okio.IOException
import org.koin.core.annotation.Singleton
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Marker that stops OkHttp from authenticating a request a second time. */
internal object PortalReauthAttempt

/** Tags a retried request so a second 401 terminates instead of looping. */
internal fun Request.withPortalReauthAttempt(): Request = newBuilder()
    .tag(PortalReauthAttempt::class.java, PortalReauthAttempt)
    .build()

/**
 * Logs in with remembered credentials when the portal client receives a 401 for an expired session.
 *
 * OkHttp calls [authenticate] from dispatcher threads, so the blocking Retrofit call is intentional.
 * A lock plus the sid captured on the failed request keeps concurrent home/payments refreshes to
 * one login: if the jar has already moved to another sid, the request is simply retried.
 */
@Singleton
class PortalAuthenticator(
    private val authApi: AuthApi,
    private val credentialsStore: CredentialsStore,
    private val sessionCookieStore: SessionCookieStore,
) : Authenticator {
    private val reauthLock = ReentrantLock()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val request = response.request
        if (request.tag(PortalReauthAttempt::class.java) != null) return null

        val failedSid = request.tag(PortalSessionAttempt::class.java)?.sid
        return reauthLock.withLock {
            val credentials = credentialsStore.read() ?: return@withLock null
            val currentSid = sessionCookieStore.sid()
            if (currentSid != null && currentSid != failedSid) {
                return@withLock request.withPortalReauthAttempt()
            }

            val login = try {
                authApi.loginCall(credentials.username, credentials.password, PORTAL_LANGUAGE).execute()
            } catch (_: IOException) {
                return@withLock null
            } catch (_: RuntimeException) {
                return@withLock null
            }

            val page = login.body()
            when {
                !login.isSuccessful -> null

                page?.isLoginPage == true -> {
                    credentialsStore.clear()
                    sessionCookieStore.clear()
                    null
                }

                page == null -> null

                else -> {
                    page.sid?.trim()?.takeIf { it.isNotEmpty() }?.let(sessionCookieStore::saveSid)
                    val newSid = sessionCookieStore.sid()
                    if (newSid == null || newSid == failedSid) {
                        null
                    } else {
                        request.withPortalReauthAttempt()
                    }
                }
            }
        }
    }

    private companion object {
        const val PORTAL_LANGUAGE = "english"
    }
}
