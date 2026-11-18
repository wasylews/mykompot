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
import dev.burnoo.kspoon.annotation.Selector
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface PortalApi {
    @FormUrlEncoded
    @POST("/index.cgi")
    suspend fun login(
        @Field("user") username: String,
        @Field("passwd") password: String,
        @Field("language") language: String,
    ): Response<ProfilePageResponse>

    @GET("/index.cgi?index=43")
    suspend fun fetchProfile(
        @Query("sid") sid: String,
    ): Response<ProfilePageResponse>

    @GET("${BuildConfig.PLANS_BASE_URL}/plans/")
    suspend fun fetchPlans(
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
    ): Response<PlansPageResponse>

    @GET("/index.cgi?index=41")
    suspend fun fetchFees(
        @Query("sid") sid: String,
        @Query("pg") pg: Int? = null,
    ): Response<FeesPageResponse>
}

@Serializable
data class ProfilePageResponse(
    @Selector(".err_message")
    val error: String? = null,
    @Selector("form[name=form_login]", attr = "action")
    val loginFormAction: String? = null,
    @Selector(".top_status li.tCenter")
    val status: String? = null,
    @Selector("table.form:nth-child(2) tr:nth-child(2) > td:nth-child(2)")
    val plan: String? = null,
    @Selector("div.kabinet-styled_table-wrap_form h3")
    val nextPayment: String? = null,
    @Selector("input[name=sid]", attr = "value")
    val sid: String? = null,
) {
    val accountState: String?
        get() = status?.removePrefix("State: ")?.trim()

    /**
     * The portal serves its login page instead of account data when the session is no longer valid.
     */
    val isLoginPage: Boolean
        get() = loginFormAction != null
}

@Serializable
data class PlansPageResponse(
    @Selector(".ptp-pricing-table .ptp-col")
    val items: List<ParsedPlanItem> = emptyList(),
)

@Serializable
data class ParsedPlanItem(
    @Selector(".ptp-plan")
    val name: String = "",
    @Selector(".ptp-price")
    val price: String = "",
    @Selector(".ptp-bullet-item", regex = "(\\d+)")
    val speed: Int = 0,
)

@Serializable
data class FeesPageResponse(
    @Selector(".err_message")
    val error: String? = null,
    @Selector("form[name=form_login]", attr = "action")
    val loginFormAction: String? = null,
    @Selector("#kabinet-filter_table tr.odd, #kabinet-filter_table tr.even")
    val items: List<ParsedFeeItem> = emptyList(),
) {
    /**
     * The portal serves its login page instead of the payments table when the session is no longer
     * valid.
     */
    val isLoginPage: Boolean
        get() = loginFormAction != null
}

@Serializable
data class ParsedFeeItem(
    @Selector("td:nth-child(1)")
    val date: String = "",
    @Selector("td:nth-child(2)")
    val description: String = "",
    @Selector("td:nth-child(3)")
    val sum: String = "",
)
