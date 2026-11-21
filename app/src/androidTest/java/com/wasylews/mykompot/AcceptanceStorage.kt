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

import com.wasylews.mykompot.data.LocalStorage
import com.wasylews.mykompot.data.SessionCookieStore
import com.wasylews.mykompot.domain.model.Credentials
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import com.wasylews.mykompot.domain.model.TariffPlans
import com.wasylews.mykompot.domain.repository.CredentialsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.koin.test.get

internal fun BaseAcceptanceTest.givenStoredSession(
    sid: String = "test-session",
    profile: Profile? = null,
) = runBlocking {
    get<SessionCookieStore>().saveSid(sid)
    if (profile != null) get<LocalStorage>().saveProfile(profile)
}

internal fun BaseAcceptanceTest.givenStoredTariffPlans(plans: List<TariffPlan> = ProfileFixtures.tariffPlans()) = runBlocking {
    get<LocalStorage>().saveTariffPlans(TariffPlans(plans))
}

internal fun BaseAcceptanceTest.givenStoredPayments(payments: Payments) = runBlocking {
    get<LocalStorage>().savePayments(payments)
}

internal fun BaseAcceptanceTest.givenStoredCredentials(
    username: String = "user",
    password: String = "pass",
) = runBlocking {
    get<CredentialsStore>().save(Credentials(username, password))
}

internal fun BaseAcceptanceTest.assertRememberedCredentials(
    username: String = "user",
    password: String = "pass",
) = runBlocking {
    assertEquals(Credentials(username, password), get<CredentialsStore>().read())
}

internal fun BaseAcceptanceTest.assertNoRememberedCredentials() = runBlocking {
    assertNull(get<CredentialsStore>().read())
}

internal fun BaseAcceptanceTest.givenPaymentDueHome() {
    givenStoredSession(profile = ProfileFixtures.paymentDueProfile())
    givenHomeResponses(profileBody = HtmlFixtures.paymentDue())
}

internal fun BaseAcceptanceTest.assertSessionCleared() = runBlocking {
    assertNull(get<SessionCookieStore>().sidFlow().first())
    assertNull(get<LocalStorage>().profileFlow().first())
}

internal fun BaseAcceptanceTest.assertLogoutClearedLocalState() = runBlocking {
    assertNull(get<SessionCookieStore>().sidFlow().first())
    assertNull(get<LocalStorage>().profileFlow().first())
    assertNull(get<LocalStorage>().paymentsFlow().first())
    assertNull(get<CredentialsStore>().read())
}

internal fun BaseAcceptanceTest.assertStoredTariffPlans(expected: List<TariffPlan>) = runBlocking {
    assertEquals(TariffPlans(expected), get<LocalStorage>().tariffPlansFlow().first())
}
