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
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
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
import java.time.LocalDate
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

    private val dispatcher = TestDispatcher()

    @Before
    fun setup() {
        runBlocking {
            getKoin().get<DataStore<ProfileEntity?>>(named("profile")).updateData { null }
            getKoin().get<DataStore<String?>>(named("session")).updateData { null }
            getKoin().get<DataStore<List<TariffPlanEntity>?>>(named("tariffPlans")).updateData { null }
        }
        loadKoinModules(testModule)
        resetServerResponses()
        server.dispatcher = dispatcher
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

    protected fun givenStoredTariffPlans(plans: List<TariffPlan> = defaultTariffPlans()) = runBlocking {
        get<LocalStorage>().saveTariffPlans(plans)
    }

    protected fun givenPaymentDueProfileStored() = givenStoredSession(profile = paymentDueProfile())

    protected fun givenPaymentDueHome() {
        givenPaymentDueProfileStored()
        givenHomeResponses(profileBody = HtmlFixtures.paymentDue())
    }

    protected fun defaultTariffPlan() = TariffPlan(
        id = "300GRN_100mb",
        name = "Standart 100",
        speed = 100,
        price = 300,
    )

    protected fun defaultTariffPlans(): List<TariffPlan> = listOf(
        TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
        TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
        TariffPlan(id = "350GRN_300mb", name = "LUX 300", speed = 300, price = 350),
    )

    protected fun allGoodProfile(tariffPlan: TariffPlan = defaultTariffPlan()) = Profile(
        isActive = true,
        nextPaymentDate = LocalDate.of(2026, 11, 1),
        tariffPlan = tariffPlan,
    )

    protected fun paymentDueProfile(tariffPlan: TariffPlan = defaultTariffPlan()) = Profile(
        isActive = true,
        nextPaymentDate = LocalDate.of(2026, 10, 2),
        tariffPlan = tariffPlan,
    )

    protected fun performLogin() {
        assertShows(R.string.username).performTextInput("user")
        assertShows(R.string.password).performTextInput("pass")
        assertShows(R.string.log_in).performClick()
    }

    protected fun assertShows(
        @StringRes res: Int,
        vararg formatArgs: Any,
    ): SemanticsNodeInteraction {
        val string = stringRes(res, *formatArgs)
        composeRule.waitUntilExactlyOneExists(hasText(string), 5_000)
        return composeRule.onNodeWithText(string).assertIsDisplayed()
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
        dispatcher.enqueue(builder.build())
    }

    protected fun givenHomeResponses(
        profileBody: String = HtmlFixtures.profile(),
        plansBody: String = HtmlFixtures.plans(),
        profileStatus: Int = 200,
        plansStatus: Int = 200,
        profileHeaders: Map<String, String> = emptyMap(),
        profileDelayMillis: Long = 0,
        plansDelayMillis: Long = 0,
    ) {
        dispatcher.profileResponseFactory = {
            response(
                body = profileBody,
                status = profileStatus,
                headers = profileHeaders,
                delayMillis = profileDelayMillis,
            )
        }
        dispatcher.plansResponseFactory = {
            response(
                body = plansBody,
                status = plansStatus,
                delayMillis = plansDelayMillis,
            )
        }
    }

    protected fun givenServerReturnsProfile(
        body: String = HtmlFixtures.profile(),
        headers: Map<String, String> = emptyMap(),
        delayMillis: Long = 0,
    ) {
        givenHomeResponses(
            profileBody = body,
            profileHeaders = headers,
            profileDelayMillis = delayMillis,
        )
    }

    protected fun givenServerReturnsLoginSuccess(sid: String = "test-session") {
        givenServerReturnsProfile(headers = mapOf("Set-Cookie" to "sid=$sid"))
    }

    protected fun givenServerIsDown(status: Int = 502) {
        givenHomeResponses(profileStatus = status, plansStatus = status)
    }

    protected fun givenServerIsUnreachable(
        body: String = HtmlFixtures.profile(),
        delayMillis: Long = 0,
    ) {
        dispatcher.profileResponseFactory = {
            MockResponse.Builder()
                .body(body)
                .onResponseBody(SocketEffect.ShutdownConnection)
                .apply {
                    if (delayMillis > 0) bodyDelay(delayMillis, TimeUnit.MILLISECONDS)
                }
                .build()
        }
        dispatcher.plansResponseFactory = { response(body = HtmlFixtures.plans()) }
    }

    private fun response(
        body: String = "",
        status: Int = 200,
        headers: Map<String, String> = emptyMap(),
        delayMillis: Long = 0,
    ): MockResponse = MockResponse.Builder()
        .code(status)
        .body(body)
        .apply {
            headers.forEach { (name, value) -> addHeader(name, value) }
            if (delayMillis > 0) bodyDelay(delayMillis, TimeUnit.MILLISECONDS)
        }
        .build()

    protected fun assertRefreshRequested() {
        assertProfileRefreshRequested()
    }

    protected fun assertProfileRefreshRequested() {
        assertNotNull(takeRequestMatching { path -> path?.contains("index.cgi") == true })
    }

    protected fun assertPlansRefreshRequested() {
        assertNotNull(takeRequestMatching { path -> path?.contains("/plans") == true })
    }

    protected fun takeRequestMatching(predicate: (String?) -> Boolean): RecordedRequest? {
        repeat(10) {
            val request = server.takeRequest(2, TimeUnit.SECONDS) ?: return null
            if (predicate(request.url.encodedPath)) return request
        }
        return null
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
        assertShows(res)
    }

    protected fun stringRes(
        res: Int,
        vararg formatArgs: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(res, *formatArgs)

    protected fun resetServerResponses() {
        dispatcher.clear()
        dispatcher.profileResponseFactory = { MockResponse.Builder().code(404).build() }
        dispatcher.plansResponseFactory = { MockResponse.Builder().body(HtmlFixtures.plans()).build() }
    }

    private class TestDispatcher : Dispatcher() {
        private val queuedResponses = ArrayDeque<MockResponse>()

        @Volatile
        var profileResponseFactory: () -> MockResponse = { MockResponse.Builder().code(404).build() }

        @Volatile
        var plansResponseFactory: () -> MockResponse = { MockResponse.Builder().body(HtmlFixtures.plans()).build() }

        fun enqueue(response: MockResponse) {
            synchronized(queuedResponses) { queuedResponses.addLast(response) }
        }

        fun clear() {
            synchronized(queuedResponses) { queuedResponses.clear() }
        }

        override fun dispatch(request: RecordedRequest): MockResponse {
            synchronized(queuedResponses) {
                if (queuedResponses.isNotEmpty()) return queuedResponses.removeFirst()
            }
            val path = request.url.encodedPath
            return when {
                path.contains("/plans") -> plansResponseFactory()
                path.contains("index.cgi") -> profileResponseFactory()
                else -> MockResponse.Builder().code(404).build()
            }
        }

        override fun close() {
            clear()
        }
    }
}
