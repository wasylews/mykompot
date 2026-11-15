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
    fun profile() = load("profile.html")

    fun paymentDue() = load("payment-due.html")

    fun suspended() = load("suspended.html")

    fun loginFailed() = load("login-failed.html")

    fun plans() = load("plans.html")

    private fun load(name: String): String = requireNotNull(
        HtmlFixtures::class.java.getResourceAsStream("/html/$name"),
    ) {
        "Missing HTML fixture: $name"
    }.bufferedReader().use { it.readText() }
}
