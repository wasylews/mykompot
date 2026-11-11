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

import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import org.junit.Test
import java.time.LocalDate

class ReturningUserRefreshesStatusTest : BaseAcceptanceTest() {
    @Test
    fun showsCachedStatusWhenOffline() {
        // GIVEN a stored session, a cached profile, and no network
        givenStoredSession(profile = allGoodProfile())
        givenServerIsUnreachable()

        launch().use {
            // THEN I see the cached status without an error
            assertShows(R.string.status_all_good)
            assertRefreshRequested()
        }
    }

    @Test
    fun showsCachedStatusWhenServerFails() {
        // GIVEN a stored session, a cached profile, and the server fails
        givenStoredSession(profile = allGoodProfile())
        givenServerIsDown()

        launch().use {
            // THEN I see the cached status without an error
            assertShows(R.string.status_all_good)
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
        // AND the server returns a profile
        givenServerReturnsProfile()

        launch().use {
            // THEN I see the status
            assertShows(R.string.status_all_good)
            assertRefreshRequested()
        }
    }

    @Test
    fun showsSuspendedStatusFetchedFromServer() {
        // GIVEN a stored session with no cached profile
        givenStoredSession()
        // AND the server returns a suspended profile
        givenServerResponse(body = HtmlFixtures.suspended())

        launch().use {
            // THEN I see the suspended status without a next charge date
            assertShows(R.string.status_suspended)
            assertDoesNotShow(R.string.due)
            assertRefreshRequested()
        }
    }

    @Test
    fun showsUnavailableWhenNoCacheAndOffline() {
        // GIVEN a stored session with no cached profile and no network
        givenStoredSession()
        givenServerIsUnreachable()

        launch().use {
            // THEN I see an error
            assertShows(R.string.status_unavailable)
            assertRefreshRequested()
        }
    }

    @Test
    fun showsLoginWhenSessionIsExpired() {
        // GIVEN a stored session
        givenStoredSession(profile = allGoodProfile())
        // AND the server treats it as unauthenticated
        givenServerResponse(body = HtmlFixtures.loginFailed())

        launch().use {
            // THEN I am taken back to login
            assertShows(R.string.username)
            assertRefreshRequested()
        }
    }

    private fun allGoodProfile() = Profile(
        isActive = true,
        nextPaymentDate = LocalDate.of(2026, 11, 1),
        tariffPlan = defaultTariff(),
    )

    private fun paymentDueProfile() = Profile(
        isActive = true,
        nextPaymentDate = LocalDate.of(2026, 10, 2),
        tariffPlan = defaultTariff(),
    )

    private fun defaultTariff() = TariffPlan(
        name = "[35] 300GRN_100mb",
        speed = 100,
        price = 300,
    )
}
