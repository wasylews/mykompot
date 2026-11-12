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
import androidx.compose.ui.test.onNodeWithText
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalTestApi::class)
class ReturningUserSkipsLoginTest : BaseAcceptanceTest() {
    @Test
    fun skipsLoginAndShowsStatus() {
        // GIVEN a saved session
        givenServerReturnsLoginSuccess()
        launch().use {
            performLogin()
            assertShows(R.string.status_all_good)
        }

        // WHEN I reopen the app
        givenServerReturnsProfile()
        launch().use {
            // THEN I see the status without logging in
            assertShows(R.string.status_all_good)
            assertLoginFormIsHidden()
            assertRefreshRequested()
        }
    }

    @Test
    fun showsPaymentDueStatus() {
        // GIVEN a saved session with a payment-due profile
        givenStoredSession(
            profile = Profile(
                isActive = true,
                nextPaymentDate = LocalDate.of(2026, 10, 2),
                tariffPlan = TariffPlan(
                    name = "[35] 300GRN_100mb",
                    speed = 100,
                    price = 300,
                ),
            ),
        )
        // AND refresh returns the same payment-due profile
        givenServerResponse(body = HtmlFixtures.paymentDue())

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

    private fun assertLoginFormIsHidden() {
        composeRule
            .onNodeWithText(stringRes(R.string.username))
            .assertDoesNotExist()
    }
}
