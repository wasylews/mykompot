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

class RestoreSessionTest : BaseAcceptanceTest() {
    @Test
    fun restoresRememberedCredentialsWhenSessionIsMissing() {
        // GIVEN remembered credentials but no stored session
        givenStoredCredentials()
        givenServerReturnsLoginSuccess()
        givenHomeResponses()

        launch().use {
            // WHEN the app starts
            // THEN it restores the session and shows the home screen
            assertShows(R.string.status_all_good)
            assertLoginRequested()
            assertProfileRequestUsesSessionCookie("test-session")
            assertRememberedCredentials()
        }
    }

    @Test
    fun returnsToLoginWhenRememberedCredentialsAreRejected() {
        // GIVEN remembered credentials the portal rejects
        givenStoredCredentials(username = "bad-user", password = "bad-pass")
        givenRememberedCredentialsAreRejected()

        launch().use {
            // WHEN the app starts
            // THEN it forgets the credentials and shows the login screen
            assertShows(R.string.username)
            assertLoginRequested()
            assertNoRememberedCredentials()
            assertSessionCleared()
        }
    }
}
