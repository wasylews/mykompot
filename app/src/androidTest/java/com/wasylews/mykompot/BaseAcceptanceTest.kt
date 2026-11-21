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
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.datastore.core.DataStore
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.wasylews.mykompot.data.SessionCookieStore
import com.wasylews.mykompot.data.model.PaymentDueNotifiedEntity
import com.wasylews.mykompot.data.model.PaymentsEntity
import com.wasylews.mykompot.data.model.ProfileEntity
import com.wasylews.mykompot.data.model.TariffPlansEntity
import com.wasylews.mykompot.domain.NotificationsPermission
import com.wasylews.mykompot.domain.repository.CredentialsStore
import com.wasylews.mykompot.presentation.main.MainActivity
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
abstract class BaseAcceptanceTest : KoinTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    internal val context = InstrumentationRegistry.getInstrumentation().targetContext

    protected val server = MockWebServer()

    protected val clock = MutableTestClock(Instant.parse("2026-10-01T10:00:00Z"))

    /** Controls what the app sees when it checks whether it may post notifications. */
    protected var notificationsGranted = true

    private val testModule = module {
        single<Clock> { clock }
        single<NotificationsPermission> { NotificationsPermission { notificationsGranted } }
    }

    internal val dispatcher = TestDispatcher()

    internal var expectedProfileRefreshes = 0
    internal var expectedPaymentsRefreshes = 0
    internal var expectedPlansRefreshes = 0

    @Before
    fun setup() {
        expectedProfileRefreshes = 0
        expectedPaymentsRefreshes = 0
        expectedPlansRefreshes = 0
        grantNotificationPermission()
        cancelAllNotifications()
        runBlocking {
            getKoin().get<DataStore<ProfileEntity?>>(named("profile")).updateData { null }
            getKoin().get<SessionCookieStore>().clear()
            getKoin().get<CredentialsStore>().clear()
            getKoin().get<DataStore<TariffPlansEntity?>>(named("tariffPlans")).updateData { null }
            getKoin().get<DataStore<PaymentsEntity?>>(named("payments")).updateData { null }
            getKoin().get<DataStore<PaymentDueNotifiedEntity?>>(named("paymentDueNotified")).updateData { null }
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

    internal class TestDispatcher : Dispatcher() {
        private val queuedResponses = ArrayDeque<MockResponse>()
        private val recordedRequests = mutableListOf<RecordedRequest>()

        fun recordedRequests(): List<RecordedRequest> = synchronized(recordedRequests) { recordedRequests.toList() }

        @Volatile
        var profileResponseFactory: (RecordedRequest) -> MockResponse = { MockResponse.Builder().code(404).build() }

        @Volatile
        var loginResponseFactory: (RecordedRequest) -> MockResponse = { request -> profileResponseFactory(request) }

        @Volatile
        var plansResponseFactory: () -> MockResponse = { MockResponse.Builder().body(HtmlFixtures.plans()).build() }

        @Volatile
        var feesResponseFactory: (RecordedRequest) -> MockResponse = { _ ->
            MockResponse.Builder().body(HtmlFixtures.fees()).build()
        }

        fun enqueue(response: MockResponse) {
            synchronized(queuedResponses) { queuedResponses.addLast(response) }
        }

        fun clear() {
            synchronized(queuedResponses) { queuedResponses.clear() }
            synchronized(recordedRequests) { recordedRequests.clear() }
        }

        override fun dispatch(request: RecordedRequest): MockResponse {
            synchronized(recordedRequests) { recordedRequests.add(request) }
            synchronized(queuedResponses) {
                if (queuedResponses.isNotEmpty()) return queuedResponses.removeFirst()
            }
            val path = request.url.encodedPath
            val index = request.url.queryParameter(FEES_PAGE_QUERY_PARAM)
            return when {
                path.contains("/plans") -> plansResponseFactory()

                path.contains("index.cgi") && index == FEES_PAGE_INDEX -> feesResponseFactory(request)

                path.contains("index.cgi") && request.method == "POST" -> {
                    val body = request.body?.utf8().orEmpty()
                    val cookie = request.headers["Cookie"].orEmpty()
                    if ("language=english" !in body || "language=english" !in cookie) {
                        MockResponse.Builder()
                            .code(400)
                            .body("Missing language")
                            .build()
                    } else {
                        loginResponseFactory(request)
                    }
                }

                path.contains("index.cgi") -> profileResponseFactory(request)

                else -> MockResponse.Builder().code(404).build()
            }
        }

        override fun close() {
            clear()
        }
    }

    companion object {
        const val FEES_PAGE_QUERY_PARAM = "index"
        const val FEES_PAGE_INDEX = "41"
    }
}
