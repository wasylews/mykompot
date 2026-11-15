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

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.datastore.core.DataStore
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.wasylews.mykompot.data.LocalStorage
import com.wasylews.mykompot.data.ProfileEntity
import com.wasylews.mykompot.data.TariffPlanEntity
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.presentation.home.HOME_LOADING_TEST_TAG
import com.wasylews.mykompot.presentation.home.HOME_SCREEN_TEST_TAG
import com.wasylews.mykompot.presentation.main.MainActivity
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
abstract class BaseAcceptanceTest : KoinTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    protected val server = MockWebServer()

    protected val clock = MutableClock(Instant.parse("2026-10-01T10:00:00Z"))
    private val testModule = module {
        single<Clock> { clock }
    }

    @Before
    fun setup() {
        runBlocking {
            getKoin().get<DataStore<ProfileEntity?>>(named("profile")).updateData { null }
            getKoin().get<DataStore<String?>>(named("session")).updateData { null }
            getKoin().get<DataStore<List<TariffPlanEntity>?>>(named("tariffPlans")).updateData { null }
        }
        loadKoinModules(testModule)
        server.start(8080)
    }

    @After
    fun teardown() {
        server.close()
        unloadKoinModules(testModule)
    }

    protected fun launch(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    protected fun givenStoredSession(
        sid: String = "test-session",
        profile: Profile? = null,
    ) = runBlocking {
        val storage = get<LocalStorage>()
        storage.saveSession(sid)
        if (profile != null) storage.saveProfile(profile)
    }

    protected fun givenStoredTariffPlans(
        plans: List<TariffPlan> = listOf(
            TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
            TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
            TariffPlan(id = "350GRN_300mb", name = "LUX 300", speed = 300, price = 350),
        ),
    ) = runBlocking {
        get<LocalStorage>().saveTariffPlans(plans)
    }

    protected fun performLogin() {
        waitForNodeWithStringRes(R.string.username).performTextInput("user")
        waitForNodeWithStringRes(R.string.password).performTextInput("pass")
        waitForNodeWithStringRes(R.string.log_in).performClick()
    }

    protected fun waitForNodeWithStringRes(
        res: Int,
        timeoutMillis: Long = 5_000,
        vararg formatArgs: Any,
    ): SemanticsNodeInteraction {
        val string = stringRes(res, *formatArgs)
        composeRule.waitUntilExactlyOneExists(hasText(string), timeoutMillis)
        return composeRule.onNodeWithText(string)
    }

    protected fun assertShows(
        @StringRes res: Int,
        vararg formatArgs: Any,
    ) {
        waitForNodeWithStringRes(res, formatArgs = formatArgs).assertIsDisplayed()
    }

    protected fun assertDoesNotShow(
        @StringRes res: Int,
    ) {
        composeRule
            .onNodeWithText(stringRes(res))
            .assertDoesNotExist()
    }

    protected fun givenServerResponse(
        body: String = "",
        status: Int = 200,
        headers: Map<String, String> = emptyMap(),
    ) {
        val builder = MockResponse.Builder()
            .code(status)
            .body(body)
        headers.forEach { (name, value) -> builder.addHeader(name, value) }
        server.enqueue(builder.build())
    }

    protected fun givenServerReturnsProfile() {
        givenServerResponse(body = HtmlFixtures.profile())
    }

    protected fun givenServerReturnsProfileAfter(delayMillis: Long = 2_000) {
        server.enqueue(
            MockResponse.Builder()
                .body(HtmlFixtures.profile())
                .bodyDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    protected fun givenServerReturnsLoginSuccess(sid: String = "test-session") {
        givenServerResponse(
            body = HtmlFixtures.profile(),
            headers = mapOf("Set-Cookie" to "sid=$sid"),
        )
    }

    protected fun givenServerIsDown(status: Int = 502) {
        givenServerResponse(status = status)
    }

    protected fun givenServerIsUnreachable(
        body: String = HtmlFixtures.profile(),
        delayMillis: Long = 0,
    ) {
        val builder = MockResponse.Builder()
            .body(body)
            .onResponseBody(SocketEffect.ShutdownConnection)
        if (delayMillis > 0) {
            builder.bodyDelay(delayMillis, TimeUnit.MILLISECONDS)
        }
        server.enqueue(builder.build())
    }

    protected fun assertRefreshRequested() {
        assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
    }

    protected fun pullToRefresh() {
        composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_SCREEN_TEST_TAG), 5_000)
        composeRule.onNodeWithTag(HOME_SCREEN_TEST_TAG).performTouchInput { swipeDown() }
    }

    protected fun assertShowsHomeLoading() {
        composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_LOADING_TEST_TAG), 5_000)
        composeRule.onNodeWithTag(HOME_LOADING_TEST_TAG).assertIsDisplayed()
    }

    protected fun assertShowsRefreshError(
        @StringRes res: Int,
    ) {
        waitForNodeWithStringRes(res).assertIsDisplayed()
    }

    protected fun stringRes(
        res: Int,
        vararg formatArgs: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(res, *formatArgs)
}
