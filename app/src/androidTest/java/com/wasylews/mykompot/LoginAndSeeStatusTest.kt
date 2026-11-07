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
import mockwebserver3.MockResponse
import mockwebserver3.SocketEffect
import org.junit.Test
import kotlin.time.Duration.Companion.days

class LoginAndSeeStatusTest : BaseAcceptanceTest() {
    @Test
    fun loginAndSeeAllGood() {
        // GIVEN server is up
        server.enqueue(MockResponse(body = HtmlFixtures.profile()))

        launch().use {
            // WHEN I log in
            performLogin()

            // THEN I see the status
            waitForNodeWithStringRes(R.string.status_all_good).assertIsDisplayed()
        }
    }

    @Test
    fun loginAndSeePaymentDue() {
        // GIVEN server is up
        server.enqueue(MockResponse(body = HtmlFixtures.profile()))
        // AND current date set past due date
        clock.advanceBy(30.days)

        launch().use {
            // WHEN I log in
            performLogin()

            // THEN I see the payment due
            waitForNodeWithStringRes(R.string.status_payment_due).assertIsDisplayed()
        }
    }

    @Test
    fun loginFailed() {
        // GIVEN server is up
        server.enqueue(MockResponse(body = HtmlFixtures.loginFailed()))

        launch().use {
            // WHEN I log in with wrong credentials
            performLogin()

            // THEN I see the error message
            waitForNodeWithStringRes(R.string.error_invalid_credentials).assertIsDisplayed()
        }
    }

    @Test
    fun connectionIssues() {
        // GIVEN no connection
        val response = MockResponse.Builder()
            .body(HtmlFixtures.profile())
            .onResponseBody(SocketEffect.ShutdownConnection)
            .build()
        server.enqueue(response)

        launch().use {
            // WHEN I try to log in
            performLogin()

            // THEN I see the error message
            waitForNodeWithStringRes(R.string.error_no_connection).assertIsDisplayed()
        }
    }

    @Test
    fun serverIssues() {
        // GIVEN server is down
        server.enqueue(MockResponse(502))

        launch().use {
            // WHEN I try to log in
            performLogin()

            // THEN I see the error message
            waitForNodeWithStringRes(R.string.error_server).assertIsDisplayed()
        }
    }
}
