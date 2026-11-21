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

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.wasylews.mykompot.data.SessionCookieStore
import com.wasylews.mykompot.data.model.PaymentDueNotifiedEntity
import com.wasylews.mykompot.data.model.PaymentsEntity
import com.wasylews.mykompot.data.model.ProfileEntity
import com.wasylews.mykompot.data.model.TariffPlansEntity
import com.wasylews.mykompot.domain.NotificationsPermission
import com.wasylews.mykompot.domain.model.Credentials
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import com.wasylews.mykompot.domain.repository.CredentialsStore
import com.wasylews.mykompot.presentation.home.HOME_LOADING_TEST_TAG
import com.wasylews.mykompot.presentation.home.HOME_REFRESH_INDICATOR_TEST_TAG
import com.wasylews.mykompot.presentation.home.HOME_SCREEN_TEST_TAG
import com.wasylews.mykompot.presentation.login.REMEMBER_ME_TEST_TAG
import com.wasylews.mykompot.presentation.main.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
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

    protected val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    protected val server = MockWebServer()

    protected val clock = MutableTestClock(Instant.parse("2026-10-01T10:00:00Z"))

    /**
     * Controls what the app sees when it checks whether it may post notifications.
     */
    protected var notificationsGranted = true

    private val testModule = module {
        single<Clock> { clock }
        single<NotificationsPermission> { NotificationsPermission { notificationsGranted } }
    }

    private val dispatcher = TestDispatcher()

    private var expectedProfileRefreshes = 0
    private var expectedPaymentsRefreshes = 0
    private var expectedPlansRefreshes = 0

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

    protected fun givenStoredSession(
        sid: String = "test-session",
        profile: Profile? = null,
    ) = runBlocking {
        get<SessionCookieStore>().saveSid(sid)
        if (profile != null) get<LocalStorage>().saveProfile(profile)
    }

    protected fun givenStoredTariffPlans(plans: List<TariffPlan> = defaultTariffPlans()) = runBlocking {
        get<LocalStorage>().saveTariffPlans(TariffPlans(plans))
    }

    protected fun givenStoredPayments(payments: Payments) = runBlocking {
        get<LocalStorage>().savePayments(payments)
    }

    protected fun givenStoredCredentials(
        username: String = "user",
        password: String = "pass",
    ) = runBlocking {
        get<CredentialsStore>().save(Credentials(username, password))
    }

    protected fun assertRememberedCredentials(
        username: String = "user",
        password: String = "pass",
    ) = runBlocking {
        assertEquals(Credentials(username, password), get<CredentialsStore>().read())
    }

    protected fun assertNoRememberedCredentials() = runBlocking {
        assertNull(get<CredentialsStore>().read())
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

    protected fun performLogin(rememberMe: Boolean = false) {
        assertShows(R.string.username).performTextInput("user")
        assertShows(R.string.password).performTextInput("pass")
        if (rememberMe) {
            composeRule.onNodeWithTag(REMEMBER_ME_TEST_TAG).performClick()
        }
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
        feesBody: String = HtmlFixtures.fees(),
        profileStatus: Int = 200,
        plansStatus: Int = 200,
        feesStatus: Int = 200,
        profileHeaders: Map<String, String> = emptyMap(),
        profileDelayMillis: Long = 0,
        plansDelayMillis: Long = 0,
        feesDelayMillis: Long = 0,
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
        dispatcher.feesResponseFactory = { _ ->
            response(
                body = feesBody,
                status = feesStatus,
                delayMillis = feesDelayMillis,
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
            profileHeaders = mapOf("Set-Cookie" to "sid=test-session") + headers,
            profileDelayMillis = delayMillis,
        )
    }

    protected fun givenServerReturnsLoginSuccess(sid: String = "test-session") {
        dispatcher.loginResponseFactory = {
            response(
                body = HtmlFixtures.profile(),
                headers = mapOf("Set-Cookie" to "sid=$sid"),
            )
        }
    }

    protected fun givenExpiredSessionWithAutoLogin(
        oldSid: String = "old-session",
        newSid: String = "new-session",
    ) {
        givenExpiredProfileResponses(oldSid, newSid)
        givenServerReturnsLoginSuccess(newSid)
    }

    protected fun givenExpiredSessionWithInvalidCredentials(
        oldSid: String = "old-session",
        newSid: String = "new-session",
    ) {
        givenExpiredProfileResponses(oldSid, newSid)
        givenRememberedCredentialsAreRejected()
    }

    protected fun givenRememberedCredentialsAreRejected() {
        dispatcher.loginResponseFactory = { response(body = HtmlFixtures.loginFailed()) }
    }

    private fun givenExpiredProfileResponses(
        oldSid: String,
        newSid: String,
    ) {
        dispatcher.profileResponseFactory = { request ->
            if ("sid=$oldSid" in request.headers["Cookie"].orEmpty()) {
                response(body = HtmlFixtures.loginFailed())
            } else {
                response(
                    body = HtmlFixtures.profile(),
                    headers = mapOf("Set-Cookie" to "sid=$newSid"),
                )
            }
        }
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
        expectedProfileRefreshes++
        assertRequestCount({ it.isProfileRefresh() }, expectedProfileRefreshes)
    }

    protected fun assertProfileRequestUsesSessionCookie(sid: String) {
        awaitRequestCondition(5_000) { countRequests { it.isProfileRefresh() } > 0 }
        val request = dispatcher.recordedRequests().first { it.isProfileRefresh() }
        assertRequestUsesSessionCookie(request, sid)
    }

    protected fun assertMostRecentProfileRequestUsesSessionCookie(sid: String) {
        awaitRequestCondition(5_000) { countRequests { it.isProfileRefresh() } > 0 }
        val request = dispatcher.recordedRequests().last { it.isProfileRefresh() }
        assertRequestUsesSessionCookie(request, sid)
    }

    private fun assertRequestUsesSessionCookie(
        request: RecordedRequest,
        sid: String,
    ) {
        val cookies = request.headers["Cookie"].orEmpty()
        check("sid=$sid" in cookies) { "Expected Cookie header with sid=$sid but was: $cookies" }
        check(request.url.queryParameter("sid") == null) {
            "sid must not be sent as a query parameter: ${request.url}"
        }
    }

    protected fun assertLoginRequested(expected: Int = 1) {
        assertRequestCount({ it.method == "POST" && it.url.encodedPath.contains("index.cgi") }, expected)
    }

    protected fun assertPaymentsRefreshRequested() {
        expectedPaymentsRefreshes++
        assertRequestCount({ it.isPaymentsRefresh() }, expectedPaymentsRefreshes)
    }

    protected fun assertNoAdditionalProfileRequest(timeoutMillis: Long = 1_000) {
        val baseline = countRequests { it.isProfileRefresh() }
        val additionalRequest = awaitRequestCondition(timeoutMillis) {
            countRequests { it.isProfileRefresh() } > baseline
        }
        if (additionalRequest) fail("Expected no additional profile refresh request")
    }

    protected fun assertPlansRefreshRequested() {
        expectedPlansRefreshes++
        assertRequestCount({ it.url.encodedPath.contains("/plans") }, expectedPlansRefreshes)
    }

    protected fun assertPlansRequestHasNoSessionCookie() {
        awaitRequestCondition(5_000) { countRequests { it.url.encodedPath.contains("/plans") } > 0 }
        val request = dispatcher.recordedRequests().first { it.url.encodedPath.contains("/plans") }
        val cookies = request.headers["Cookie"].orEmpty()
        check("sid=" !in cookies) { "Plans request must not carry the session cookie: $cookies" }
    }

    private fun assertRequestCount(
        predicate: (RecordedRequest) -> Boolean,
        expected: Int,
        timeoutMillis: Long = 20_000,
    ) {
        awaitRequestCondition(timeoutMillis) { countRequests(predicate) >= expected }
        val actual = countRequests(predicate)
        if (actual < expected) {
            fail("Expected at least $expected matching requests but found $actual")
        }
    }

    private fun countRequests(predicate: (RecordedRequest) -> Boolean): Int = dispatcher.recordedRequests().count(predicate)

    private fun awaitRequestCondition(
        timeoutMillis: Long,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(50)
        }
        return false
    }

    private fun RecordedRequest.isLoginRequest(): Boolean = url.encodedPath.contains("index.cgi") &&
        method == "POST"

    private fun RecordedRequest.isProfileRefresh(): Boolean = url.encodedPath.contains("index.cgi") &&
        method == "GET" &&
        url.queryParameter(FEES_PAGE_QUERY_PARAM) != FEES_PAGE_INDEX

    private fun RecordedRequest.isPaymentsRefresh(): Boolean = url.encodedPath.contains("index.cgi") &&
        method == "GET" &&
        url.queryParameter(FEES_PAGE_QUERY_PARAM) == FEES_PAGE_INDEX

    protected fun pullToRefresh() {
        composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_SCREEN_TEST_TAG), 5_000)
        composeRule.onNodeWithTag(HOME_SCREEN_TEST_TAG).performTouchInput { swipeDown() }
    }

    protected fun assertShowsHomeLoading() {
        composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_LOADING_TEST_TAG), 5_000)
        composeRule.onNodeWithTag(HOME_LOADING_TEST_TAG).assertIsDisplayed()
    }

    protected fun assertShowsRefreshIndicator() {
        composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_REFRESH_INDICATOR_TEST_TAG), 5_000)
        composeRule.onNodeWithTag(HOME_REFRESH_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    protected fun assertNoRefreshIndicator() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(HOME_REFRESH_INDICATOR_TEST_TAG).fetchSemanticsNodes().isEmpty()
        }
    }

    protected fun assertShowsRefreshError(
        @StringRes res: Int,
    ) {
        assertShows(res)
    }

    protected fun stringRes(
        res: Int,
        vararg formatArgs: Any,
    ): String = context.getString(res, *formatArgs)

    protected fun assertSessionCleared() = runBlocking {
        assertNull(get<SessionCookieStore>().sidFlow().first())
        assertNull(get<LocalStorage>().profileFlow().first())
    }

    protected fun assertLogoutClearedLocalState() = runBlocking {
        assertNull(get<SessionCookieStore>().sidFlow().first())
        assertNull(get<LocalStorage>().profileFlow().first())
        assertNull(get<LocalStorage>().paymentsFlow().first())
        assertNull(get<CredentialsStore>().read())
    }

    protected fun assertStoredTariffPlans(expected: List<TariffPlan>) = runBlocking {
        assertEquals(TariffPlans(expected), get<LocalStorage>().tariffPlansFlow().first())
    }

    protected fun assertLoginRequestDoesNotUseSessionCookie(sid: String) {
        awaitRequestCondition(5_000) { countRequests { it.isLoginRequest() } > 0 }
        val request = dispatcher.recordedRequests().last { it.isLoginRequest() }
        val cookies = request.headers["Cookie"].orEmpty()
        check("sid=$sid" !in cookies) { "Old session cookie was still sent: $cookies" }
    }

    protected fun assertNoNotificationPosted() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(0, notificationManager.activeNotifications.size)
    }

    protected fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                InstrumentationRegistry.getInstrumentation().targetContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    private fun cancelAllNotifications() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    protected fun assertNotificationPosted(
        @StringRes titleRes: Int,
        @StringRes textRes: Int,
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val expectedTitle = stringRes(titleRes)
        val expectedText = stringRes(textRes)

        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5)
        var posted: Notification? = null
        while (System.currentTimeMillis() < deadline) {
            posted = notificationManager.activeNotifications
                .map { it.notification }
                .firstOrNull { notification ->
                    val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    title == expectedTitle && text == expectedText
                }
            if (posted != null) break
            Thread.sleep(100)
        }

        assertNotNull("Expected a notification to be posted", posted)
        assertNotNull("Expected notification to open the app", posted?.contentIntent)
    }

    protected fun resetServerResponses() {
        dispatcher.clear()
        dispatcher.profileResponseFactory = { MockResponse.Builder().code(404).build() }
        dispatcher.loginResponseFactory = { request -> dispatcher.profileResponseFactory(request) }
        dispatcher.plansResponseFactory = { MockResponse.Builder().body(HtmlFixtures.plans()).build() }
        dispatcher.feesResponseFactory = { _ -> MockResponse.Builder().body(HtmlFixtures.fees()).build() }
    }

    protected fun givenFeesPages(
        page1: String = HtmlFixtures.fees(),
        page2: String = HtmlFixtures.feesPage2(),
        page2DelayMillis: Long = 0,
    ) {
        dispatcher.feesResponseFactory = { request ->
            val pg = request.url.queryParameter("pg")
            if (pg == "25") {
                response(body = page2, delayMillis = page2DelayMillis)
            } else {
                response(body = page1)
            }
        }
    }

    private class TestDispatcher : Dispatcher() {
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

    private companion object {
        const val FEES_PAGE_QUERY_PARAM = "index"
        const val FEES_PAGE_INDEX = "41"
    }
}
