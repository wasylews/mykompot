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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.presentation.home.HOME_LIST_TEST_TAG
import com.wasylews.mykompot.presentation.home.PAYMENTS_LOADING_MORE_TEST_TAG
import com.wasylews.mykompot.presentation.home.PAYMENTS_SKELETON_TEST_TAG
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PaymentsListTest : BaseAcceptanceTest() {
    @Test
    fun showsPaymentsUnderStatusCard() {
        // GIVEN a stored session, tariff catalog, profile and payments from a slow first page
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenHomeResponses(feesDelayMillis = 4_000)

        launch().use {
            // WHEN I open the home screen
            // THEN the first page shows skeleton rows instead of an empty state
            composeRule.waitUntilExactlyOneExists(hasTestTag(PAYMENTS_SKELETON_TEST_TAG), 5_000)
            assertShows(R.string.status_all_good)
            assertShows(R.string.payments)

            // AND I see payment entries under the status card with date, amount and tariff name from price
            composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_LIST_TEST_TAG), 5_000)
            composeRule.onNodeWithTag(HOME_LIST_TEST_TAG).assertIsDisplayed()
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithText("300", substring = true)
                    .fetchSemanticsNodes().size > 1
            }
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithText("Standart 100", substring = true)
                    .fetchSemanticsNodes().size > 1
            }
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithTag(PAYMENTS_SKELETON_TEST_TAG).fetchSemanticsNodes().isEmpty()
            }
        }
    }

    @Test
    fun keepsCachedPaymentsVisibleDuringBackgroundRefresh() {
        // GIVEN cached payments and a slow background refresh
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenStoredPayments(Payments(items = listOf(PaymentFixtures.payment()), hasMore = false))
        givenHomeResponses(feesDelayMillis = 3_000)

        launch().use {
            assertShows(R.string.status_all_good)
            assertShows(R.string.payments)
            assertPaymentsRefreshRequested()

            // THEN cached rows stay visible instead of being replaced by skeleton
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithText("1 Oct, 10:05", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(
                composeRule.onAllNodesWithTag(PAYMENTS_SKELETON_TEST_TAG).fetchSemanticsNodes().isEmpty(),
            )
            assertTrue(
                composeRule.onAllNodesWithTag(PAYMENTS_LOADING_MORE_TEST_TAG).fetchSemanticsNodes().isEmpty(),
            )
        }
    }

    @Test
    fun showsPaymentsErrorAndRetriesWhenFirstPageFailsWithoutCache() {
        // GIVEN a stored session with no cached payments and a failing first page
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenHomeResponses(feesStatus = 502)

        launch().use {
            // THEN I see an actionable payments error
            assertShows(R.string.status_all_good)
            assertShows(R.string.error_server)

            // WHEN the server recovers and I retry
            givenHomeResponses(feesStatus = 200)
            assertShows(R.string.retry).performClick()

            // THEN the payments are shown and the error is gone
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithText("1 Oct, 10:05", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertDoesNotShow(R.string.error_server)
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

            // WHEN I scroll past the status card
            composeRule.onNodeWithTag(HOME_LIST_TEST_TAG).performTouchInput { swipeUp() }

            // THEN the status card stays pinned above the payments
            assertShows(R.string.status_all_good)
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

    @Test
    fun showsSkeletonWhileLoadingNextPaymentsPage() {
        // GIVEN paginated payments whose next page is slow
        givenStoredSession(profile = allGoodProfile())
        givenStoredTariffPlans()
        givenHomeResponses()
        givenFeesPages(page2DelayMillis = 2_000)

        launch().use {
            assertShows(R.string.status_all_good)
            assertShows(R.string.payments)
            composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_LIST_TEST_TAG), 5_000)
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithText("1 Oct, 10:05", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // WHEN I scroll to the end of the first page to trigger the next page
            composeRule.onNodeWithTag(HOME_LIST_TEST_TAG).performScrollToIndex(26)

            // THEN skeleton rows stand in for the incoming payments
            composeRule.waitUntilExactlyOneExists(hasTestTag(PAYMENTS_LOADING_MORE_TEST_TAG), 5_000)

            // AND the skeleton disappears once the next page arrives
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithTag(PAYMENTS_LOADING_MORE_TEST_TAG).fetchSemanticsNodes().isEmpty()
            }
        }
    }
}
