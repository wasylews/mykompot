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

/**
 * Retrofit client for the Kompot customer portal.
 *
 * The portal is a legacy CGI application served as HTML from `index.cgi` pages
 * (authenticated pages are selected by the `index` query parameter). Plans are served from a
 * separate public `/plans/` page on [BuildConfig.PLANS_BASE_URL].
 *
 * Session state is carried by a `sid` cookie returned in `Set-Cookie` and managed by a `CookieJar`.
 * When a session is no longer valid, the portal responds with its login page instead of the
 * requested data.
 *
 * The UI language of portal responses depends on the selected language; this client requests
 * English content (`language=english` and `Cookie: language=english`) so parsing can rely on stable
 * English labels such as `State: Enable`.
 */
interface PortalApi {
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

    /**
     * Fetches the Internet/service profile page (`index=43`), which contains account status, tariff,
     * and next payment information. Authentication is carried by the `sid` cookie.
     */
    @GET("/index.cgi?index=43")
    suspend fun fetchProfile(): Response<ProfilePageResponse>

    /**
     * Fetches the public tariff plans page.
     *
     * Supports conditional requests: a `304 Not Modified` response means the cached catalog is still
     * current when [ifNoneMatch]/[ifModifiedSince] validators are provided.
     */
    @GET("${BuildConfig.PLANS_BASE_URL}/plans/")
    suspend fun fetchPlans(
        @Header("If-None-Match") ifNoneMatch: String? = null,
        @Header("If-Modified-Since") ifModifiedSince: String? = null,
    ): Response<PlansPageResponse>

    /**
     * Fetches the fees/payments history page (`index=41`).
     *
     * Authentication is carried by the `sid` cookie.
     *
     * @param pg pagination offset/count requested by the portal; `null` fetches the first page.
     */
    @GET("/index.cgi?index=41")
    suspend fun fetchFees(
        @Query("pg") pg: Int? = null,
    ): Response<FeesPageResponse>
}

/**
 * Parsed portal page containing profile/account information, or a login/error page when the session
 * is invalid or the request is rejected.
 */
@Serializable
data class ProfilePageResponse(
    /** Portal error message shown on the page, if present. */
    @Selector(".err_message")
    val error: String? = null,
    /** Action URL of the login form; present when the portal served the login page. */
    @Selector("form[name=form_login]", attr = "action")
    val loginFormAction: String? = null,
    /** Top status bar text (for example `State: Enable` in English). */
    @Selector(".top_status li.tCenter")
    val status: String? = null,
    /** Raw tariff plan cell value from the profile table (for example `[35] 300GRN_100mb`). */
    @Selector("table.form:nth-child(2) tr:nth-child(2) > td:nth-child(2)")
    val plan: String? = null,
    /** Heading containing the next periodic payment text/date (ISO date in parentheses). */
    @Selector("div.kabinet-styled_table-wrap_form h3")
    val nextPayment: String? = null,
    /** Session token embedded in page markup, when provided. */
    @Selector("input[name=sid]", attr = "value")
    val sid: String? = null,
) {
    /** Account state label extracted from [status] (for example `Enable`). */
    val accountState: String?
        get() = status?.removePrefix("State: ")?.trim()

    /**
     * `true` when the portal served its login page instead of account data, indicating the session
     * is missing or no longer valid.
     */
    val isLoginPage: Boolean
        get() = loginFormAction != null
}

/** Parsed public plans page containing available tariff plan cards. */
@Serializable
data class PlansPageResponse(
    @Selector(".ptp-pricing-table .ptp-col")
    val items: List<ParsedPlanItem> = emptyList(),
)

/** Raw tariff plan data scraped from a plan card on the plans page. */
@Serializable
data class ParsedPlanItem(
    /** Display name of the plan. */
    @Selector(".ptp-plan")
    val name: String = "",
    /** Price label (for example `300 грн./міс.`); used to detect monthly plans and extract cost. */
    @Selector(".ptp-price")
    val price: String = "",
    /** Plan speed in Mbps extracted from the plan's bullet items. */
    @Selector(".ptp-bullet-item", regex = "(\\d+)")
    val speed: Int = 0,
)

/**
 * Parsed fees/payments history page, or a login/error page when the session is invalid or the
 * request is rejected.
 */
@Serializable
data class FeesPageResponse(
    /** Portal error message shown on the page, if present. */
    @Selector(".err_message")
    val error: String? = null,
    /** Action URL of the login form; present when the portal served the login page. */
    @Selector("form[name=form_login]", attr = "action")
    val loginFormAction: String? = null,
    /** Payment rows from the fees table. */
    @Selector("#kabinet-filter_table tr.odd, #kabinet-filter_table tr.even")
    val items: List<ParsedFeeItem> = emptyList(),
) {
    /**
     * `true` when the portal served its login page instead of the payments table, indicating the
     * session is missing or no longer valid.
     */
    val isLoginPage: Boolean
        get() = loginFormAction != null
}

/** Raw payment row scraped from the fees table. */
@Serializable
data class ParsedFeeItem(
    /** Payment date/time as shown by the portal (formatted as `yyyy-MM-dd HH:mm:ss`). */
    @Selector("td:nth-child(1)")
    val date: String = "",
    /** Payment description/source identifier from the portal. */
    @Selector("td:nth-child(2)")
    val description: String = "",
    /** Payment amount as shown by the portal. */
    @Selector("td:nth-child(3)")
    val sum: String = "",
)
