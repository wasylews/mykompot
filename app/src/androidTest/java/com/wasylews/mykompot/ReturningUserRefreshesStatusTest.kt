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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.wasylews.mykompot.presentation.home.STATUS_TARIFF_NAME_TEST_TAG
import com.wasylews.mykompot.presentation.home.STATUS_TARIFF_PRICE_TEST_TAG
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReturningUserRefreshesStatusTest : BaseAcceptanceTest() {
    @Test
    fun showsCachedStatusWhenServerFails() {
        // GIVEN a stored session, a cached profile, and the server fails
        givenStoredSession(profile = ProfileFixtures.allGoodProfile())
        givenServerIsDown()

        launch().use {
            // THEN I see the cached status without losing it
            assertShows(R.string.status_all_good)
            assertDoesNotShow(R.string.status_unknown)
            // AND a non-blocking refresh error with a way to retry without losing the status
            assertShowsRefreshError(R.string.error_server)
            assertShows(R.string.retry)
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun updatesStatusFromServer() {
        // GIVEN a stored session with an out-of-date cached profile
        givenStoredSession(profile = ProfileFixtures.paymentDueProfile())
        // AND the server returns the current profile
        givenServerReturnsProfile()

        launch().use {
            // THEN the content updates to the server profile
            assertShows(R.string.status_all_good)
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun fetchesStatusWhenNoCache() {
        // GIVEN a stored session with no cached profile
        givenStoredSession()
        givenStoredTariffPlans()
        // AND the server returns a profile
        givenServerReturnsProfile()

        launch().use {
            // THEN I see the status
            assertShows(R.string.status_all_good)
            assertShows(R.string.tariff)
            composeRule.onNodeWithTag(STATUS_TARIFF_NAME_TEST_TAG, useUnmergedTree = true).assertExists()
            composeRule.onNodeWithTag(STATUS_TARIFF_PRICE_TEST_TAG, useUnmergedTree = true).assertExists()
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun showsOnlyLoadingSkeletonWhileFetchingWithoutCache() {
        // GIVEN a stored session with no cached profile and a slow server
        givenStoredSession()
        givenServerReturnsProfile(delayMillis = 2_000)

        launch().use {
            // THEN only the content skeleton is shown, without Unknown or a pull-to-refresh spinner
            assertShowsHomeLoading()
            assertDoesNotShow(R.string.status_unknown)
            assertNoRefreshIndicator()

            // AND the status once the server responds
            assertShows(R.string.status_all_good)
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun doesNotShowRefreshIndicatorWhileRefreshingCachedStatusOnOpen() {
        // GIVEN a stored session with a cached profile and a slow server
        givenStoredSession(profile = ProfileFixtures.allGoodProfile())
        givenServerReturnsProfile(delayMillis = 2_000)

        launch().use {
            // THEN the cached status is shown without an automatic pull-to-refresh spinner
            assertShows(R.string.status_all_good)
            assertNoRefreshIndicator()
        }
    }

    @Test
    fun showsSuspendedStatusFetchedFromServer() {
        // GIVEN a stored session with no cached profile
        givenStoredSession()
        // AND the server returns a suspended profile
        givenHomeResponses(profileBody = HtmlFixtures.suspended())

        launch().use {
            // THEN I see the suspended status without a next charge date
            assertShows(R.string.status_suspended)
            assertDoesNotShow(R.string.due)
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun showsUnknownStatusWhenNoCacheAndServerFails() {
        // GIVEN a stored session with no cached profile and the server fails
        givenStoredSession()
        givenHomeResponses(profileStatus = 502, profileDelayMillis = 2_000)

        launch().use {
            // THEN I see progress while refreshing
            assertShowsHomeLoading()
            assertDoesNotShow(R.string.status_unknown)

            // AND the unknown status with the failure reason once refresh fails with no cache
            assertShows(R.string.status_unknown)
            assertShowsRefreshError(R.string.error_server)
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun recoversFromUnknownStatusOnRetry() {
        // GIVEN a stored session with no cached profile and no network
        givenStoredSession()
        givenServerIsUnreachable(delayMillis = 2_000)

        launch().use {
            // THEN I see progress while refreshing, then the unknown status
            assertShowsHomeLoading()
            assertDoesNotShow(R.string.status_unknown)
            assertShows(R.string.status_unknown)
            assertProfileRefreshRequested()

            // WHEN the server is back and I tap retry
            givenServerReturnsProfile()
            assertShows(R.string.retry).performClick()

            // THEN I see the status
            assertShows(R.string.status_all_good)
        }
    }

    @Test
    fun showsLoginWhenSessionIsExpired() {
        // GIVEN a stored session
        givenStoredSession(profile = ProfileFixtures.allGoodProfile())
        // AND the server treats it as unauthenticated
        givenHomeResponses(profileBody = HtmlFixtures.loginFailed())

        launch().use {
            // THEN I am taken back to login
            assertShows(R.string.username)
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun keepsCachedStatusWhenPortalReturnsGenericError() {
        // GIVEN a stored session with a cached profile
        givenStoredSession(profile = ProfileFixtures.allGoodProfile())
        // AND the server returns a generic portal error page, not a login page
        givenHomeResponses(profileBody = HtmlFixtures.portalError())

        launch().use {
            // THEN I see the cached status without losing it
            assertShows(R.string.status_all_good)
            assertDoesNotShow(R.string.status_unknown)
            assertDoesNotShow(R.string.username)
            // AND a non-blocking refresh error
            assertShowsRefreshError(R.string.error_server)
            assertProfileRefreshRequested()
        }
    }

    @Test
    fun userPullShowsIndicatorAndRefreshesStatusAndPayments() {
        // GIVEN a stored session with no cached profile and no network
        givenStoredSession()
        givenServerIsUnreachable(delayMillis = 3_000)

        launch().use {
            // THEN I see the unknown status after the failed background refresh
            assertShowsHomeLoading()
            assertShows(R.string.status_unknown)
            assertProfileRefreshRequested()
            assertPaymentsRefreshRequested()

            // WHEN the server recovers and I pull to refresh
            givenServerReturnsProfile(delayMillis = 3_000)
            pullToRefresh()

            // THEN the pull-to-refresh indicator is shown only for my request
            assertShowsRefreshIndicator()
            assertNoRefreshIndicator()

            // AND both the status and payments are refreshed again
            assertProfileRefreshRequested()
            assertPaymentsRefreshRequested()
            assertShows(R.string.status_all_good)
        }
    }

    @Test
    fun userPullDuringBackgroundRefreshReusesRunningJob() {
        // GIVEN a stored session with a cached profile and a slow background refresh
        givenStoredSession(profile = ProfileFixtures.allGoodProfile())
        givenStoredTariffPlans()
        givenServerReturnsProfile(delayMillis = 3_000)

        launch().use {
            assertShows(R.string.status_all_good)
            assertNoRefreshIndicator()
            assertProfileRefreshRequested()
            assertPaymentsRefreshRequested()

            // WHEN I pull while the background refresh is still running
            pullToRefresh()

            // THEN the indicator is shown without starting a second status refresh
            assertShowsRefreshIndicator()
            assertNoAdditionalProfileRequest()

            // AND it disappears when the original refresh finishes
            assertNoRefreshIndicator()
            assertShows(R.string.status_all_good)
        }
    }

    @Test
    fun keepsCachedStatusWhenPullToRefreshFails() {
        // GIVEN a stored session with a cached profile and a working server
        givenStoredSession(profile = ProfileFixtures.allGoodProfile())
        givenServerReturnsProfile()

        launch().use {
            // THEN I see the refreshed status
            assertShows(R.string.status_all_good)
            assertProfileRefreshRequested()

            // WHEN the server fails and I pull to refresh
            givenServerIsUnreachable()
            pullToRefresh()

            // THEN the cached status stays with an inline error and a retry
            assertShows(R.string.status_all_good)
            assertDoesNotShow(R.string.status_unknown)
            assertShowsRefreshError(R.string.error_no_connection)
            assertShows(R.string.retry)
        }
    }
}
