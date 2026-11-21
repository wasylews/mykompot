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

package com.wasylews.mykompot.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.wasylews.mykompot.ProfileFixtures
import com.wasylews.mykompot.data.model.PaymentsSerializer
import com.wasylews.mykompot.data.model.ProfileEntitySerializer
import com.wasylews.mykompot.data.model.TariffPlansSerializer
import com.wasylews.mykompot.domain.model.Payment
import com.wasylews.mykompot.domain.model.PaymentSource
import com.wasylews.mykompot.domain.model.Payments
import com.wasylews.mykompot.domain.model.TariffPlans
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime

class DataStoreLocalStorageTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    @Test
    fun `profile flow emits the saved profile`() = runTest {
        val store = createLocalStorage()

        val profile = profile()
        store.saveProfile(profile)

        val savedProfile = store.profileFlow().first()
        assertEquals(profile, savedProfile)
    }

    @Test
    fun `profile flow emits the saved profile without next payment date`() = runTest {
        val store = createLocalStorage()

        val profile = profile().copy(isActive = false, nextPaymentDate = null)
        store.saveProfile(profile)

        val savedProfile = store.profileFlow().first()
        assertEquals(profile, savedProfile)
    }

    @Test
    fun `profile flow emits null when nothing is stored`() = runTest {
        val store = createLocalStorage()

        val savedProfile = store.profileFlow().first()

        assertNull(savedProfile)
    }

    @Test
    fun `clearing a profile removes it`() = runTest {
        val store = createLocalStorage()
        store.saveProfile(profile())

        store.clearProfile()

        assertNull(store.profileFlow().first())
    }

    @Test
    fun `profile without next payment date survives a new store instance`() = runTest {
        val profileFile = File(tmp.root, PROFILE_FILE)
        val profile = profile().copy(isActive = false, nextPaymentDate = null)
        val first = createLocalStorage(profileFile = profileFile)
        first.saveProfile(profile)
        closeStores()

        val second = createLocalStorage(profileFile = profileFile)

        assertEquals(profile, second.profileFlow().first())
    }

    @Test
    fun `corrupt profile reads as null`() = runTest {
        val profileFile = File(tmp.root, PROFILE_FILE)
        val first = createLocalStorage(profileFile = profileFile)
        first.saveProfile(profile())
        closeStores()

        profileFile.writeText("{not json")

        val second = createLocalStorage(profileFile = profileFile)

        assertNull(second.profileFlow().first())
    }

    @Test
    fun `corrupt tariff plans read as null`() = runTest {
        val plansFile = File(tmp.root, PLANS_FILE)
        val first = createLocalStorage(plansFile = plansFile)
        first.saveTariffPlans(TariffPlans(ProfileFixtures.tariffPlans()))
        closeStores()

        plansFile.writeText("{not json")

        val second = createLocalStorage(plansFile = plansFile)

        assertNull(second.tariffPlansFlow().first())
    }

    @Test
    fun `tariff plans flow emits null when nothing is stored`() = runTest {
        val store = createLocalStorage()

        val plans = store.tariffPlansFlow().first()

        assertNull(plans)
    }

    @Test
    fun `saving tariff plans stores them`() = runTest {
        val store = createLocalStorage()

        store.saveTariffPlans(TariffPlans(ProfileFixtures.tariffPlans()))

        assertEquals(TariffPlans(ProfileFixtures.tariffPlans()), store.tariffPlansFlow().first())
    }

    @Test
    fun `tariff plans survive a new store instance`() = runTest {
        val plansFile = File(tmp.root, PLANS_FILE)
        val first = createLocalStorage(plansFile = plansFile)
        first.saveTariffPlans(TariffPlans(ProfileFixtures.tariffPlans()))
        closeStores()

        val second = createLocalStorage(plansFile = plansFile)

        assertEquals(TariffPlans(ProfileFixtures.tariffPlans()), second.tariffPlansFlow().first())
    }

    @Test
    fun `payments flow emits null when nothing is stored`() = runTest {
        val store = createLocalStorage()

        val payments = store.paymentsFlow().first()

        assertNull(payments)
    }

    @Test
    fun `saving payments stores them`() = runTest {
        val store = createLocalStorage()

        store.savePayments(payments())

        assertEquals(payments(), store.paymentsFlow().first())
    }

    @Test
    fun `clearing payments removes them`() = runTest {
        val store = createLocalStorage()
        store.savePayments(payments())

        store.clearPayments()

        assertNull(store.paymentsFlow().first())
    }

    @Test
    fun `payments survive a new store instance`() = runTest {
        val paymentsFile = File(tmp.root, PAYMENTS_FILE)
        val first = createLocalStorage(paymentsFile = paymentsFile)
        first.savePayments(payments())
        closeStores()

        val second = createLocalStorage(paymentsFile = paymentsFile)

        assertEquals(payments(), second.paymentsFlow().first())
    }

    @Test
    fun `corrupt payments read as null`() = runTest {
        val paymentsFile = File(tmp.root, PAYMENTS_FILE)
        val first = createLocalStorage(paymentsFile = paymentsFile)
        first.savePayments(payments())
        closeStores()

        paymentsFile.writeText("{not json")

        val second = createLocalStorage(paymentsFile = paymentsFile)

        assertNull(second.paymentsFlow().first())
    }

    private fun createLocalStorage(
        profileFile: File = File(tmp.root, PROFILE_FILE),
        plansFile: File = File(tmp.root, PLANS_FILE),
        paymentsFile: File = File(tmp.root, PAYMENTS_FILE),
    ) = DataStoreLocalStorage(
        profileDataStore = createStore(
            file = profileFile,
            serializer = ProfileEntitySerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { null },
        ),
        tariffPlansDataStore = createStore(
            file = plansFile,
            serializer = TariffPlansSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { null },
        ),
        paymentsDataStore = createStore(
            file = paymentsFile,
            serializer = PaymentsSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { null },
        ),
    )

    private fun <T> createStore(
        file: File,
        serializer: Serializer<T>,
        corruptionHandler: ReplaceFileCorruptionHandler<T>,
    ): DataStore<T> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scopes += scope
        return DataStoreFactory.create(
            serializer = serializer,
            corruptionHandler = corruptionHandler,
            scope = scope,
            produceFile = { file },
        )
    }

    private suspend fun closeStores() {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
        scopes.clear()
    }

    private fun payments() = Payments(
        items = listOf(
            Payment(
                dateTime = LocalDateTime.of(2026, 10, 1, 10, 5, 8),
                amount = 300,
                tariffName = "Standart 100",
                source = PaymentSource.PRIVAT24,
            ),
            Payment(
                dateTime = LocalDateTime.of(2024, 10, 15, 9, 39, 29),
                amount = 250,
                tariffName = "250",
                source = PaymentSource.EASYPAY,
            ),
        ),
        hasMore = true,
        nextPageOffset = 25,
    )

    private companion object {
        const val PROFILE_FILE = "profile.json"
        const val PLANS_FILE = "tariff_plans.json"
        const val PAYMENTS_FILE = "payments.json"
    }
}
