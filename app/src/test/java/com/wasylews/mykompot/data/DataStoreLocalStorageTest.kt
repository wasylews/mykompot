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
import com.wasylews.mykompot.domain.model.TariffPlan
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
    fun `session flow emits null when nothing is stored`() = runTest {
        val store = createLocalStorage()

        val session = store.sessionFlow().first()

        assertNull(session)
    }

    @Test
    fun `saving a session stores it`() = runTest {
        val store = createLocalStorage()

        store.saveSession("test-session")

        assertEquals("test-session", store.sessionFlow().first())
    }

    @Test
    fun `saving a profile keeps the session`() = runTest {
        val store = createLocalStorage()
        store.saveSession("test-session")

        store.saveProfile(profile())

        assertEquals("test-session", store.sessionFlow().first())
    }

    @Test
    fun `session survives a new store instance`() = runTest {
        val sessionFile = File(tmp.root, SESSION_FILE)
        val first = createLocalStorage(sessionFile = sessionFile)
        first.saveSession("test-session")
        closeStores()

        val second = createLocalStorage(sessionFile = sessionFile)

        assertEquals("test-session", second.sessionFlow().first())
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
    fun `corrupt profile reads as null and keeps the session`() = runTest {
        val profileFile = File(tmp.root, PROFILE_FILE)
        val sessionFile = File(tmp.root, SESSION_FILE)
        val first = createLocalStorage(profileFile, sessionFile)
        first.saveSession("test-session")
        closeStores()

        profileFile.writeText("{not json")

        val second = createLocalStorage(profileFile, sessionFile)

        assertNull(second.profileFlow().first())
        assertEquals("test-session", second.sessionFlow().first())
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

        store.saveTariffPlans(tariffPlans())

        assertEquals(tariffPlans(), store.tariffPlansFlow().first())
    }

    @Test
    fun `tariff plans survive a new store instance`() = runTest {
        val plansFile = File(tmp.root, PLANS_FILE)
        val first = createLocalStorage(plansFile = plansFile)
        first.saveTariffPlans(tariffPlans())
        closeStores()

        val second = createLocalStorage(plansFile = plansFile)

        assertEquals(tariffPlans(), second.tariffPlansFlow().first())
    }

    private fun createLocalStorage(
        profileFile: File = File(tmp.root, PROFILE_FILE),
        sessionFile: File = File(tmp.root, SESSION_FILE),
        plansFile: File = File(tmp.root, PLANS_FILE),
    ) = DataStoreLocalStorage(
        profileDataStore = createStore(
            file = profileFile,
            serializer = ProfileEntitySerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { null },
        ),
        sessionDataStore = createStore(
            file = sessionFile,
            serializer = SessionSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { null },
        ),
        tariffPlansDataStore = createStore(
            file = plansFile,
            serializer = TariffPlansSerializer,
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

    private fun tariffPlans() = listOf(
        TariffPlan(name = "Econom 50", speed = 50, price = 260),
        TariffPlan(name = "Standart 100", speed = 100, price = 300),
    )

    private companion object {
        const val PROFILE_FILE = "profile.json"
        const val SESSION_FILE = "session.json"
        const val PLANS_FILE = "tariff_plans.json"
    }
}
