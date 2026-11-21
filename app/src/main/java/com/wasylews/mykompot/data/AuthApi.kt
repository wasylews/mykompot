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

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/** Retrofit client for portal authentication. Shares the session cookie jar with the data client. */
interface AuthApi {
    /**
     * Authenticates the user and returns the landing/profile page on success.
     *
     * @param username portal account login.
     * @param password portal account password.
     * @param language portal UI language (for example `english`); affects the language of the
     * returned HTML.
     */
    @FormUrlEncoded
    @POST("/index.cgi")
    suspend fun login(
        @Field("user") username: String,
        @Field("passwd") password: String,
        @Field("language") language: String,
    ): Response<ProfilePageResponse>
}
