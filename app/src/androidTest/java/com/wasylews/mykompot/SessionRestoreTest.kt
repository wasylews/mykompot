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
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.wasylews.mykompot.presentation.main.MainActivity
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class SessionRestoreTest : KoinTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val server = MockWebServer()

    private val clock = MutableClock(Instant.parse("2026-10-01T10:00:00Z"))
    private val testModule = module {
        single<Clock> { clock }
    }

    @Before
    fun setup() {
        loadKoinModules(testModule)
        server.start(8080)
    }

    @After
    fun teardown() {
        server.close()
        unloadKoinModules(testModule)
    }

    @Test
    fun reopeningTheAppKeepsTheSession() {
        // GIVEN a successful login
        server.enqueue(
            MockResponse.Builder()
                .body(HtmlFixtures.profile())
                .addHeader("Set-Cookie", "sid=test-session")
                .build(),
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            performLogin()
            waitForNodeWithStringRes(R.string.status_all_good).assertIsDisplayed()
        }

        // WHEN I reopen the app
        ActivityScenario.launch(MainActivity::class.java).use {
            // THEN I see the status without logging in
            waitForNodeWithStringRes(R.string.status_all_good).assertIsDisplayed()
            assertLoginFormIsHidden()
        }
    }

    private fun performLogin() {
        waitForNodeWithStringRes(R.string.username).performTextInput("user")
        waitForNodeWithStringRes(R.string.password).performTextInput("pass")
        waitForNodeWithStringRes(R.string.log_in).performClick()
    }

    private fun assertLoginFormIsHidden() {
        composeRule
            .onNodeWithText(string(R.string.username))
            .assertDoesNotExist()
    }

    private fun waitForNodeWithStringRes(
        res: Int,
        timeoutMillis: Long = 5_000,
    ): SemanticsNodeInteraction {
        composeRule.waitUntilExactlyOneExists(hasText(string(res)), timeoutMillis)
        return composeRule.onNodeWithText(string(res))
    }

    private fun string(res: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(res)
}
