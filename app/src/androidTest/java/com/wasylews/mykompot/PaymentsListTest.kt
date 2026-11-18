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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.wasylews.mykompot.presentation.home.HOME_LIST_TEST_TAG
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PaymentsListTest : BaseAcceptanceTest() {
    @Test
    fun showsPaymentsUnderStatusCard() {
        // GIVEN a stored session, tariff catalog, profile and payments from server
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenHomeResponses()

        launch().use {
            // WHEN I open the home screen
            assertShows(R.string.status_all_good)

            // THEN I see payment entries under the status card with date, amount and tariff name from price
            assertShows(R.string.payments)
            composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_LIST_TEST_TAG), 5_000)
            composeRule.onNodeWithTag(HOME_LIST_TEST_TAG).assertIsDisplayed()
            composeRule.waitUntil {
                composeRule.onAllNodesWithText("300", substring = true)
                    .fetchSemanticsNodes().size > 1
            }
            composeRule.waitUntil {
                composeRule.onAllNodesWithText("Standart 100", substring = true)
                    .fetchSemanticsNodes().size > 1
            }
        }
    }

    @Test
    fun keepsStatusCardPinnedWhileScrollingPayments() {
        // GIVEN a stored session, tariff catalog, profile and payments from server
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenHomeResponses()

        launch().use {
            assertShows(R.string.status_all_good)
            assertShows(R.string.payments)

            // WHEN I scroll the payments
            composeRule.onNodeWithTag(HOME_LIST_TEST_TAG).performTouchInput { swipeUp() }

            // THEN the status card stays pinned while the payments scroll away
            assertShows(R.string.status_all_good)
            assertDoesNotShow(R.string.payments)
        }
    }

    @Test
    fun loadsNextPaymentsPageOnScroll() {
        // GIVEN a stored session, tariff catalog, profile and paginated payments from server
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenHomeResponses()
        givenFeesPages()

        launch().use {
            assertShows(R.string.status_all_good)
            assertShows(R.string.payments)
            composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_LIST_TEST_TAG), 5_000)

            // WHEN I scroll to load more payments
            repeat(8) {
                composeRule.onNodeWithTag(HOME_LIST_TEST_TAG).performTouchInput { swipeUp() }
            }

            // THEN I see entries from the next page (pg=25)
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithText("15 Oct 2024, 09:39", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule
                .onNodeWithTag(HOME_LIST_TEST_TAG)
                .performScrollToNode(hasText("15 Oct 2024, 09:39", substring = true))
            composeRule
                .onAllNodesWithText("15 Oct 2024, 09:39", substring = true)
                .onFirst()
                .assertIsDisplayed()
        }
    }
}
