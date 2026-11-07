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

import androidx.compose.ui.test.onNodeWithText
import org.junit.Test

class SessionRestoreTest : BaseAcceptanceTest() {
    @Test
    fun reopeningTheAppKeepsTheSession() {
        // GIVEN a successful login
        givenServerReturnsLoginSuccess()
        launch().use {
            performLogin()
            assertShows(R.string.status_all_good)
        }

        // WHEN I reopen the app
        launch().use {
            // THEN I see the status without logging in
            assertShows(R.string.status_all_good)
            assertLoginFormIsHidden()
        }
    }

    private fun assertLoginFormIsHidden() {
        composeRule
            .onNodeWithText(stringRes(R.string.username))
            .assertDoesNotExist()
    }
}
