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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class LoginAndSeeStatusTest : KoinTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val server = MockWebServer()

    private val clock = MutableClock(Instant.parse("2026-11-01T10:00:00Z"))
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
    }

    @Test
    fun loginAndSeeAllGood() {
        // GIVEN server is up
        server.enqueue(MockResponse(body = HtmlFixtures.profile()))

        // WHEN I log in
        performLogin()

        // THEN I see the status
        waitForNodeWithStringRes(R.string.all_good).assertIsDisplayed()
    }

    @Test
    fun loginAndSeePaymentDue() {
        // GIVEN server is up
        server.enqueue(MockResponse(body = HtmlFixtures.profile()))
        // AND current date set past due date
        clock.advanceBy(10.days)

        // WHEN I log in
        performLogin()

        // THEN I see the payment due
        waitForNodeWithStringRes(R.string.payment_due).assertIsDisplayed()
    }

    @Test
    fun loginFailed() {
        // GIVEN server is up
        server.enqueue(MockResponse(body = HtmlFixtures.loginFailed()))

        // WHEN I log in with wrong credentials
        performLogin()

        // THEN I see the error message
        waitForNodeWithStringRes(R.string.error_invalid_credentials).assertIsDisplayed()
    }

    @Test
    fun connectionIssues() {
        // GIVEN no connection
        val response = MockResponse.Builder()
            .body(HtmlFixtures.profile())
            .onResponseBody(SocketEffect.ShutdownConnection)
            .build()
        server.enqueue(response)

        // WHEN I try to log in
        performLogin()

        // THEN I see the error message
        waitForNodeWithStringRes(R.string.error_no_connection).assertIsDisplayed()
    }

    @Test
    fun serverIssues() {
        // GIVEN server is down
        server.enqueue(MockResponse(502))

        // WHEN I try to log in
        performLogin()

        // THEN I see the error message
        waitForNodeWithStringRes(R.string.error_server).assertIsDisplayed()
    }

    private fun performLogin() {
        waitForNodeWithStringRes(R.string.username).performTextInput("user")
        waitForNodeWithStringRes(R.string.password).performTextInput("pass")
        waitForNodeWithStringRes(R.string.log_in).performClick()
    }

    private fun waitForNodeWithStringRes(
        res: Int,
        timeoutMillis: Long = 5_000,
    ): SemanticsNodeInteraction {
        val string = composeRule.activity.getString(res)
        composeRule.waitUntilExactlyOneExists(hasText(string), timeoutMillis)
        return composeRule.onNodeWithText(string)
    }
}
