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

package com.wasylews.mykompot

object HtmlFixtures {
    private const val SID_PLACEHOLDER = "{{SID}}"
    private const val DEFAULT_SID = "test-session"

    fun profile(sid: String = DEFAULT_SID) = load("profile.html", sid)

    fun paymentDue(sid: String = DEFAULT_SID) = load("payment-due.html", sid)

    fun suspended(sid: String = DEFAULT_SID) = load("suspended.html", sid)

    fun loginFailed() = load("login-failed.html")

    fun portalError() = load("portal-error.html")

    fun plans() = load("plans.html")

    fun fees(sid: String = DEFAULT_SID) = load("fees.html", sid)

    fun feesPage2(sid: String = DEFAULT_SID) = load("fees-page2.html", sid)

    private fun load(
        name: String,
        sid: String = DEFAULT_SID,
    ): String = requireNotNull(
        HtmlFixtures::class.java.getResourceAsStream("/html/$name"),
    ) {
        "Missing HTML fixture: $name"
    }.bufferedReader().use { it.readText() }
        .replace(SID_PLACEHOLDER, sid)
}
