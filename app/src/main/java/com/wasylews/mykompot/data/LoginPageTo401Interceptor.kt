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
import okhttp3.Response
import org.koin.core.annotation.Singleton

/**
 * Converts a portal login page served with `200 OK` into `401 Unauthorized` so OkHttp's
 * authenticator can handle session expiry.
 *
 * The body is only peeked, leaving it readable by the HTML converter. Attached as a network
 * interceptor to the portal data client only, so OkHttp's retry layer sees the 401 and invokes the
 * authenticator. Login and public plans responses must stay as they are.
 */
@Singleton
class LoginPageTo401Interceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code != HTTP_OK || LOGIN_FORM_MARKER !in response.peekBody(BODY_LIMIT_BYTES).string()) {
            return response
        }
        return response.newBuilder()
            .code(HTTP_UNAUTHORIZED)
            .message("Unauthorized")
            .build()
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val LOGIN_FORM_MARKER = "form_login"
        const val BODY_LIMIT_BYTES = 64L * 1024
    }
}
