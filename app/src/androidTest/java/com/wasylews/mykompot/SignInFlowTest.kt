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

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.wasylews.mykompot.presentation.home.STATUS_TARIFF_NAME_TEST_TAG
import com.wasylews.mykompot.presentation.home.STATUS_TARIFF_PRICE_TEST_TAG
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
            composeRule.onNodeWithTag(STATUS_TARIFF_NAME_TEST_TAG, useUnmergedTree = true).assertExists()
            composeRule.onNodeWithTag(STATUS_TARIFF_PRICE_TEST_TAG, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun signInSucceedsWhenPortalSendsSidOnlyInHtml() {
        // GIVEN a real portal-style login response: the sid is in the HTML, not a Set-Cookie header
        givenStoredTariffPlans()
        givenHomeResponses(profileBody = HtmlFixtures.profile(sid = "html-session"))

        launch().use {
            // WHEN I log in
            performLogin()

            // THEN the session is established and I see the status
            assertShows(R.string.status_all_good)
            assertProfileRequestUsesSessionCookie("html-session")
        }
    }

    @Test
    fun remembersCredentialsWhenAsked() {
        // GIVEN server is up
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        launch().use {
            // WHEN I opt in and log in
            performLogin(rememberMe = true)

            // THEN my credentials are saved for automatic re-login
            assertShows(R.string.status_all_good)
            assertRememberedCredentials()
        }
    }

    @Test
    fun doesNotRememberCredentialsByDefault() {
        // GIVEN server is up
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        launch().use {
            // WHEN I log in without opting in
            performLogin()

            // THEN no credentials are saved
            assertShows(R.string.status_all_good)
            assertNoRememberedCredentials()
        }
    }

    @Test
    fun forgetsRememberedCredentialsWhenLoginFails() {
        // GIVEN remembered credentials that cannot be restored while the server is unavailable
        givenStoredCredentials(username = "old-user", password = "old-pass")
        givenServerResponse(status = 502)
        givenServerResponse(body = HtmlFixtures.loginFailed())

        launch().use {
            // WHEN I try to log in again and the portal rejects the credentials
            performLogin(rememberMe = true)

            // THEN the stale credentials are not kept
            assertShows(R.string.error_invalid_credentials)
            assertNoRememberedCredentials()
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

    @Test
    fun asksForNotificationPermissionAfterSignIn() {
        // GIVEN notifications are not allowed yet
        notificationsGranted = false
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        launch().use {
            // WHEN I log in
            performLogin()

            // THEN I am told why the app wants to notify me
            assertShows(R.string.status_all_good)
            assertShows(R.string.notifications_rationale_title)
            assertShows(R.string.notifications_allow)
            assertShows(R.string.notifications_not_now)
        }
    }

    @Test
    fun doesNotAskForNotificationPermissionWhenAlreadyAllowed() {
        // GIVEN notifications are already allowed
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        launch().use {
            // WHEN I log in
            performLogin()

            // THEN I land on the status without an extra prompt
            assertShows(R.string.status_all_good)
            assertDoesNotShow(R.string.notifications_rationale_title)
        }
    }

    @Test
    fun dismissesNotificationRationaleWithoutBlockingTheApp() {
        // GIVEN notifications are not allowed yet
        notificationsGranted = false
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        launch().use {
            // WHEN I decline the prompt
            performLogin()
            assertShows(R.string.notifications_not_now).performClick()

            // THEN the prompt closes and the app keeps working
            assertDoesNotShow(R.string.notifications_rationale_title)
            assertShows(R.string.status_all_good)
        }
    }
}
