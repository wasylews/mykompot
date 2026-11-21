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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.wasylews.mykompot.presentation.home.STATUS_TARIFF_NAME_TEST_TAG
import com.wasylews.mykompot.presentation.home.STATUS_TARIFF_PRICE_TEST_TAG
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReturningUserSkipsLoginTest : BaseAcceptanceTest() {
    @Test
    fun skipsLoginAndShowsStatus() {
        // GIVEN a stored session, tariff catalog, and a server profile
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        // WHEN I open the app
        launch().use {
            // THEN I see the status without logging in
            assertShows(R.string.status_all_good)
            assertShows(R.string.tariff)
            composeRule.onNodeWithTag(STATUS_TARIFF_NAME_TEST_TAG, useUnmergedTree = true).assertExists()
            composeRule.onNodeWithTag(STATUS_TARIFF_PRICE_TEST_TAG, useUnmergedTree = true).assertExists()
            assertLoginFormIsHidden()
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun profileRefreshUsesSessionCookieInsteadOfSidQuery() {
        // GIVEN a stored session
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        // WHEN I open the app
        launch().use {
            // THEN the profile request carries the session cookie, not a query parameter
            assertShows(R.string.status_all_good)
            assertProfileRequestUsesSessionCookie("test-session")
        }
    }

    @Test
    fun showsPaymentDueStatus() {
        // GIVEN a saved session with a payment-due profile and refresh returns it
        givenPaymentDueHome()

        launch().use {
            // WHEN I open the app
            // THEN I see the payment due status without logging in
            assertShows(R.string.status_payment_due)
            val due = stringRes(R.string.due)
            composeRule.waitUntilExactlyOneExists(hasText(due, ignoreCase = true), 5_000)
            composeRule.onNodeWithText(due, ignoreCase = true).assertIsDisplayed()
            assertLoginFormIsHidden()
            assertRefreshRequested()
        }
    }

    @Test
    fun asksForNotificationPermissionWhenOpeningWithStoredSession() {
        // GIVEN a stored session and notifications are not allowed yet
        notificationsGranted = false
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        launch().use {
            // THEN I see the status and the notification prompt without logging in
            assertShows(R.string.status_all_good)
            assertShows(R.string.notifications_rationale_title)
            assertLoginFormIsHidden()
        }
    }

    private fun assertLoginFormIsHidden() {
        composeRule
            .onNodeWithText(stringRes(R.string.username))
            .assertDoesNotExist()
    }
}
