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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LoginAndSeeStatusTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val server = MockWebServer()

    @Before
    fun setup() {
        server.start(8080)
    }

    @After
    fun teardown() {
        server.close()
    }

    @Test
    fun loginAndSeeAllGood() {
        // GIVEN server is up
        server.enqueue(MockResponse(body = HtmlFixtures.profile()))

        // WHEN I log in
        composeRule.onNodeWithText("Username").performTextInput("user")
        composeRule.onNodeWithText("Password").performTextInput("pass")
        composeRule.onNodeWithText("Log in").performClick()

        // THEN I see the status
        composeRule.waitUntilExactlyOneExists(
            hasText("All Good"),
        )
        composeRule.onNodeWithText("All Good").assertIsDisplayed()
    }
}
