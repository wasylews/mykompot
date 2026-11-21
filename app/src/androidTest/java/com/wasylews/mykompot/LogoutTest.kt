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

import androidx.compose.ui.test.performClick
import com.wasylews.mykompot.domain.model.Payments
import org.junit.Test

class LogoutTest : BaseAcceptanceTest() {
    @Test
    fun logoutClearsLocalStateAndReturnsToLogin() {
        // GIVEN a signed-in user with remembered credentials and cached portal data
        givenStoredSession(sid = "old-session", profile = allGoodProfile())
        givenStoredCredentials()
        givenStoredTariffPlans()
        givenStoredPayments(Payments(items = emptyList(), hasMore = false))
        givenHomeResponses()

        launch().use {
            assertShows(R.string.status_all_good)

            // WHEN I log out
            assertShows(R.string.log_out).performClick()

            // THEN only local session state is forgotten and the login form is shown
            assertShows(R.string.username)
            assertLogoutClearedLocalState()
            assertStoredTariffPlans(defaultTariffPlans())
        }
    }

    @Test
    fun logoutPreventsOldSessionCookieFromBeingReused() {
        // GIVEN a signed-in user
        givenStoredSession(sid = "old-session", profile = allGoodProfile())
        givenHomeResponses()

        launch().use {
            assertShows(R.string.status_all_good)
            assertShows(R.string.log_out).performClick()
            assertShows(R.string.username)

            // WHEN I sign in again
            performLogin()

            // THEN the previous session cookie is not sent to the portal
            assertLoginRequestDoesNotUseSessionCookie("old-session")
        }
    }
}
