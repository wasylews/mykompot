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

import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import org.junit.Assert.fail
import java.util.concurrent.TimeUnit

internal fun response(
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

internal fun BaseAcceptanceTest.resetServerResponses() {
    dispatcher.clear()
    dispatcher.profileResponseFactory = { MockResponse.Builder().code(404).build() }
    dispatcher.loginResponseFactory = { request -> dispatcher.profileResponseFactory(request) }
    dispatcher.plansResponseFactory = { MockResponse.Builder().body(HtmlFixtures.plans()).build() }
    dispatcher.feesResponseFactory = { _ -> MockResponse.Builder().body(HtmlFixtures.fees()).build() }
}

internal fun BaseAcceptanceTest.givenServerResponse(
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

internal fun BaseAcceptanceTest.givenHomeResponses(
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

internal fun BaseAcceptanceTest.givenServerReturnsProfile(
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

internal fun BaseAcceptanceTest.givenServerReturnsLoginSuccess(sid: String = "test-session") {
    dispatcher.loginResponseFactory = {
        response(
            body = HtmlFixtures.profile(),
            headers = mapOf("Set-Cookie" to "sid=$sid"),
        )
    }
}

internal fun BaseAcceptanceTest.givenExpiredSessionWithAutoLogin(
    oldSid: String = "old-session",
    newSid: String = "new-session",
) {
    givenExpiredProfileResponses(oldSid, newSid)
    givenServerReturnsLoginSuccess(newSid)
}

internal fun BaseAcceptanceTest.givenExpiredSessionWithInvalidCredentials(
    oldSid: String = "old-session",
    newSid: String = "new-session",
) {
    givenExpiredProfileResponses(oldSid, newSid)
    givenRememberedCredentialsAreRejected()
}

internal fun BaseAcceptanceTest.givenRememberedCredentialsAreRejected() {
    dispatcher.loginResponseFactory = { response(body = HtmlFixtures.loginFailed()) }
}

private fun BaseAcceptanceTest.givenExpiredProfileResponses(
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

internal fun BaseAcceptanceTest.givenServerIsDown(status: Int = 502) {
    givenHomeResponses(profileStatus = status, plansStatus = status)
}

internal fun BaseAcceptanceTest.givenServerIsUnreachable(
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

internal fun BaseAcceptanceTest.givenFeesPages(
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

internal fun BaseAcceptanceTest.assertRefreshRequested() {
    assertProfileRefreshRequested()
}

internal fun BaseAcceptanceTest.assertProfileRefreshRequested() {
    expectedProfileRefreshes++
    assertRequestCount({ it.isProfileRefresh() }, expectedProfileRefreshes)
}

internal fun BaseAcceptanceTest.assertProfileRequestUsesSessionCookie(sid: String) {
    awaitRequestCondition(5_000) { countRequests { it.isProfileRefresh() } > 0 }
    val request = dispatcher.recordedRequests().first { it.isProfileRefresh() }
    assertRequestUsesSessionCookie(request, sid)
}

internal fun BaseAcceptanceTest.assertMostRecentProfileRequestUsesSessionCookie(sid: String) {
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

internal fun BaseAcceptanceTest.assertLoginRequested(expected: Int = 1) {
    assertRequestCount({ it.method == "POST" && it.url.encodedPath.contains("index.cgi") }, expected)
}

internal fun BaseAcceptanceTest.assertPaymentsRefreshRequested() {
    expectedPaymentsRefreshes++
    assertRequestCount({ it.isPaymentsRefresh() }, expectedPaymentsRefreshes)
}

internal fun BaseAcceptanceTest.assertNoAdditionalProfileRequest(timeoutMillis: Long = 1_000) {
    val baseline = countRequests { it.isProfileRefresh() }
    val additionalRequest = awaitRequestCondition(timeoutMillis) {
        countRequests { it.isProfileRefresh() } > baseline
    }
    if (additionalRequest) fail("Expected no additional profile refresh request")
}

internal fun BaseAcceptanceTest.assertPlansRefreshRequested() {
    expectedPlansRefreshes++
    assertRequestCount({ it.url.encodedPath.contains("/plans") }, expectedPlansRefreshes)
}

internal fun BaseAcceptanceTest.assertPlansRequestHasNoSessionCookie() {
    awaitRequestCondition(5_000) { countRequests { it.url.encodedPath.contains("/plans") } > 0 }
    val request = dispatcher.recordedRequests().first { it.url.encodedPath.contains("/plans") }
    val cookies = request.headers["Cookie"].orEmpty()
    check("sid=" !in cookies) { "Plans request must not carry the session cookie: $cookies" }
}

internal fun BaseAcceptanceTest.assertLoginRequestDoesNotUseSessionCookie(sid: String) {
    awaitRequestCondition(5_000) { countRequests { it.isLoginRequest() } > 0 }
    val request = dispatcher.recordedRequests().last { it.isLoginRequest() }
    val cookies = request.headers["Cookie"].orEmpty()
    check("sid=$sid" !in cookies) { "Old session cookie was still sent: $cookies" }
}

private fun BaseAcceptanceTest.assertRequestCount(
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

private fun BaseAcceptanceTest.countRequests(predicate: (RecordedRequest) -> Boolean): Int = dispatcher.recordedRequests().count(predicate)

private fun BaseAcceptanceTest.awaitRequestCondition(
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
    url.queryParameter(BaseAcceptanceTest.FEES_PAGE_QUERY_PARAM) != BaseAcceptanceTest.FEES_PAGE_INDEX

private fun RecordedRequest.isPaymentsRefresh(): Boolean = url.encodedPath.contains("index.cgi") &&
    method == "GET" &&
    url.queryParameter(BaseAcceptanceTest.FEES_PAGE_QUERY_PARAM) == BaseAcceptanceTest.FEES_PAGE_INDEX
