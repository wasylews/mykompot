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

class ReturningUserRefreshesStatusTest : BaseAcceptanceTest() {
    @Test
    fun showsCachedStatusWhenOffline() {
        // GIVEN a stored session, a cached profile, and no network
        givenStoredSession(profile = allGoodProfile())
        givenServerIsUnreachable()

        launch().use {
            // THEN I see the cached status without losing it
            assertShows(R.string.status_all_good)
            assertDoesNotShow(R.string.status_unknown)
            // AND a non-blocking refresh error
            assertShowsRefreshError(R.string.error_no_connection)
            assertRefreshRequested()
        }
    }

    @Test
    fun showsCachedStatusWhenServerFails() {
        // GIVEN a stored session, a cached profile, and the server fails
        givenStoredSession(profile = allGoodProfile())
        givenServerIsDown()

        launch().use {
            // THEN I see the cached status without losing it
            assertShows(R.string.status_all_good)
            assertDoesNotShow(R.string.status_unknown)
            // AND a non-blocking refresh error
            assertShowsRefreshError(R.string.error_server)
            assertRefreshRequested()
        }
    }

    @Test
    fun updatesStatusFromServer() {
        // GIVEN a stored session with an out-of-date cached profile
        givenStoredSession(profile = paymentDueProfile())
        // AND the server returns the current profile
        givenServerReturnsProfile()

        launch().use {
            // THEN the content updates to the server profile
            assertShows(R.string.status_all_good)
            assertRefreshRequested()
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
            composeRule.onNodeWithText("Standart 100").assertIsDisplayed()
            assertShows(R.string.tariff_price, 300)
            assertRefreshRequested()
        }
    }

    @Test
    fun showsLoadingWhileFetchingWithoutCache() {
        // GIVEN a stored session with no cached profile and a slow server
        givenStoredSession()
        givenServerReturnsProfile(delayMillis = 2_000)

        launch().use {
            // THEN I see progress instead of the unknown status while refreshing
            assertShowsHomeLoading()
            assertDoesNotShow(R.string.status_unknown)

            // AND the status once the server responds
            assertShows(R.string.status_all_good)
            assertRefreshRequested()
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
            assertRefreshRequested()
        }
    }

    @Test
    fun showsUnknownStatusWhenNoCacheAndOffline() {
        // GIVEN a stored session with no cached profile and no network
        givenStoredSession()
        givenServerIsUnreachable(delayMillis = 2_000)

        launch().use {
            // THEN I see progress instead of the unknown status while refreshing
            assertShowsHomeLoading()
            assertDoesNotShow(R.string.status_unknown)

            // AND the unknown status once refresh fails with no cache
            assertShows(R.string.status_unknown)
            assertRefreshRequested()
        }
    }

    @Test
    fun recoversFromUnknownStatusOnPullToRefresh() {
        // GIVEN a stored session with no cached profile and no network
        givenStoredSession()
        givenServerIsUnreachable(delayMillis = 2_000)

        launch().use {
            // THEN I see progress while refreshing, then the unknown status
            assertShowsHomeLoading()
            assertDoesNotShow(R.string.status_unknown)
            assertShows(R.string.status_unknown)
            assertRefreshRequested()

            // WHEN the server is back and I pull to refresh
            givenServerReturnsProfile()
            pullToRefresh()

            // THEN I see the status
            assertShows(R.string.status_all_good)
            assertRefreshRequested()
        }
    }

    @Test
    fun showsLoginWhenSessionIsExpired() {
        // GIVEN a stored session
        givenStoredSession(profile = allGoodProfile())
        // AND the server treats it as unauthenticated
        givenHomeResponses(profileBody = HtmlFixtures.loginFailed())

        launch().use {
            // THEN I am taken back to login
            assertShows(R.string.username)
            assertRefreshRequested()
        }
    }

    @Test
    fun refreshesStatusOnPullToRefresh() {
        // GIVEN a stored session with a cached profile
        givenStoredSession(profile = allGoodProfile())
        givenServerReturnsProfile()
        givenServerReturnsProfile()

        launch().use {
            // THEN I see the status after the initial refresh
            assertShows(R.string.status_all_good)
            assertRefreshRequested()

            // WHEN I pull to refresh
            pullToRefresh()

            // THEN the status is refreshed again
            assertRefreshRequested()
        }
    }

    @Test
    fun keepsCachedStatusWhenPullToRefreshFails() {
        // GIVEN a stored session with a cached profile and a working server
        givenStoredSession(profile = allGoodProfile())
        givenServerReturnsProfile()

        launch().use {
            // THEN I see the refreshed status
            assertShows(R.string.status_all_good)
            assertRefreshRequested()

            // WHEN the server fails and I pull to refresh
            givenServerIsUnreachable()
            pullToRefresh()

            // THEN the cached status stays and I see a non-blocking error
            assertShows(R.string.status_all_good)
            assertDoesNotShow(R.string.status_unknown)
            assertShowsRefreshError(R.string.error_no_connection)
        }
    }


}
