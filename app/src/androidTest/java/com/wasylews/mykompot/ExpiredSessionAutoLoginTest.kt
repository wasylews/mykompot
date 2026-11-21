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

import org.junit.Test

class ExpiredSessionAutoLoginTest : BaseAcceptanceTest() {
    @Test
    fun silentlyReLoginsAndRetriesExpiredProfile() {
        // GIVEN an expired session with remembered credentials
        givenStoredSession(sid = "old-session", profile = ProfileFixtures.allGoodProfile())
        givenStoredCredentials()
        givenStoredTariffPlans()
        givenExpiredSessionWithAutoLogin()

        launch().use {
            // WHEN the app refreshes the expired profile
            // THEN it logs in once and shows the retried status
            assertShows(R.string.status_all_good)
            assertLoginRequested()
            assertMostRecentProfileRequestUsesSessionCookie("new-session")
            assertRememberedCredentials()
        }
    }

    @Test
    fun returnsToLoginWhenRememberedCredentialsAreRejected() {
        // GIVEN an expired session with credentials the server rejects
        givenStoredSession(sid = "old-session", profile = ProfileFixtures.allGoodProfile())
        givenStoredCredentials(username = "bad-user", password = "bad-pass")
        givenStoredTariffPlans()
        givenExpiredSessionWithInvalidCredentials()

        launch().use {
            // WHEN the automatic login fails
            // THEN the stale credentials and session are dropped
            assertShows(R.string.username)
            assertLoginRequested()
            assertNoRememberedCredentials()
            assertSessionCleared()
        }
    }
}
