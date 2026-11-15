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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.Test

class SignInFlowTest : BaseAcceptanceTest() {
    @Test
    fun showsLoginWhenOpenedWithoutSession() {
        // GIVEN no saved session

        launch().use {
            // THEN I see the login form
            assertShows(R.string.username)
        }
    }

    @Test
    fun signInSucceedsAndShowsStatus() {
        // GIVEN server is up
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        launch().use {
            // WHEN I log in
            performLogin()

            // THEN I see the status
            assertShows(R.string.status_all_good)
            assertShows(R.string.tariff)
            composeRule.onNodeWithText("Standart 100").assertIsDisplayed()
            assertShows(R.string.tariff_price, 300)
        }
    }

    @Test
    fun signInFailsWithInvalidCredentials() {
        // GIVEN server is up
        givenServerResponse(body = HtmlFixtures.loginFailed())

        launch().use {
            // WHEN I log in with wrong credentials
            performLogin()

            // THEN I see the error message
            assertShows(R.string.error_invalid_credentials)
        }
    }

    @Test
    fun signInFailsWhenOffline() {
        // GIVEN no connection
        givenServerIsUnreachable()

        launch().use {
            // WHEN I try to log in
            performLogin()

            // THEN I see the error message
            assertShows(R.string.error_no_connection)
        }
    }

    @Test
    fun signInFailsWhenServerUnavailable() {
        // GIVEN server is down
        givenServerIsDown()

        launch().use {
            // WHEN I try to log in
            performLogin()

            // THEN I see the error message
            assertShows(R.string.error_server)
        }
    }

    @Test
    fun signInFailsWhenResponseCannotBeParsed() {
        // GIVEN server returns a successful response without a parseable profile
        givenServerResponse(
            body = "<html></html>",
            headers = mapOf("Set-Cookie" to "sid=test-session"),
        )

        launch().use {
            // WHEN I log in
            performLogin()

            // THEN I see a parsing error
            assertShows(R.string.error_parsing)
        }
    }
}
