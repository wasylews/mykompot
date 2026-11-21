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
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.wasylews.mykompot.data.model.PaymentDueNotifiedEntity
import com.wasylews.mykompot.data.model.PaymentDueNotifiedSerializer
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStorePaymentDueNotifiedStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    @Test
    fun `nothing is notified initially`() = runTest {
        val store = createStore()

        assertFalse(store.isNotified("DUE_DAY|2026-10-01"))
    }

    @Test
    fun `mark stores the key`() = runTest {
        val store = createStore()

        store.markNotified("DUE_DAY|2026-10-01")

        assertTrue(store.isNotified("DUE_DAY|2026-10-01"))
        assertFalse(store.isNotified("THREE_DAYS_BEFORE|2026-10-01"))
    }

    @Test
    fun `marking the same key twice keeps one entry`() = runTest {
        val file = File(tmp.root, STORE_FILE)
        val store = createStore(file)

        store.markNotified("DUE_DAY|2026-10-01")
        store.markNotified("DUE_DAY|2026-10-01")
        closeStores()

        val reopened = createStore(file)

        assertTrue(reopened.isNotified("DUE_DAY|2026-10-01"))
        assertEquals(listOf("DUE_DAY|2026-10-01"), reopened.keys())
    }

    @Test
    fun `clear removes all keys`() = runTest {
        val store = createStore()
        store.markNotified("DUE_DAY|2026-10-01")
        store.markNotified("THREE_DAYS_BEFORE|2026-10-04")

        store.clear()

        assertFalse(store.isNotified("DUE_DAY|2026-10-01"))
        assertFalse(store.isNotified("THREE_DAYS_BEFORE|2026-10-04"))
    }

    @Test
    fun `keys survive a new store instance`() = runTest {
        val file = File(tmp.root, STORE_FILE)
        val first = createStore(file)
        first.markNotified("THREE_DAYS_BEFORE|2026-10-04")
        closeStores()

        val second = createStore(file)

        assertTrue(second.isNotified("THREE_DAYS_BEFORE|2026-10-04"))
    }

    @Test
    fun `corrupt file reads as nothing notified`() = runTest {
        val file = File(tmp.root, STORE_FILE)
        val first = createStore(file)
        first.markNotified("DUE_DAY|2026-10-01")
        closeStores()
        file.writeText("{not json")

        val second = createStore(file)

        assertFalse(second.isNotified("DUE_DAY|2026-10-01"))
    }

    private fun createStore(file: File = File(tmp.root, STORE_FILE)): TestStore = TestStore(
        DataStoreFactory.create(
            serializer = PaymentDueNotifiedSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { null },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it },
            produceFile = { file },
        ),
    )

    private suspend fun closeStores() {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
        scopes.clear()
    }

    private class TestStore(
        private val dataStore: DataStore<PaymentDueNotifiedEntity?>,
    ) : com.wasylews.mykompot.domain.PaymentDueNotifiedStore {
        private val delegate = DataStorePaymentDueNotifiedStore(dataStore)

        override suspend fun isNotified(key: String): Boolean = delegate.isNotified(key)

        override suspend fun markNotified(key: String) = delegate.markNotified(key)

        override suspend fun clear() = delegate.clear()

        suspend fun keys(): List<String> = dataStore.data.first()?.notifiedKeys.orEmpty()
    }

    private companion object {
        const val STORE_FILE = "payment_due_notified.json"
    }
}
