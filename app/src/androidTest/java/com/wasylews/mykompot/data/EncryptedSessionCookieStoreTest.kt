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

import android.content.Context
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedSessionCookieStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = EncryptedSessionCookieStore(context)

    @Before
    fun setUp() {
        store.clear()
        context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.dataStoreFile(LEGACY_SESSION_FILE).delete()
    }

    @Test
    fun savesReadsAndClearsSessionCookie() {
        store.saveSid("session-1")

        assertEquals("session-1", store.sid())
        runBlocking { assertEquals("session-1", store.sidFlow().first()) }

        store.clear()

        assertNull(store.sid())
        runBlocking { assertNull(store.sidFlow().first()) }
    }

    @Test
    fun sessionCookieSurvivesNewStoreInstance() {
        store.saveSid("session-2")

        assertEquals("session-2", EncryptedSessionCookieStore(context).sid())
    }

    @Test
    fun doesNotStorePlaintextSessionCookie() {
        store.saveSid("secret-session")

        val rawValues = context
            .getSharedPreferences(EncryptedSessionCookieStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .all
            .values
            .joinToString(" ")

        assertFalse("session cookie leaked: $rawValues", "secret-session" in rawValues)
    }

    @Test
    fun migratesLegacyPlainCookieAndClearsIt() {
        context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SID, "legacy-cookie")
            .commit()

        val migrated = EncryptedSessionCookieStore(context)

        assertEquals("legacy-cookie", migrated.sid())
        assertNull(
            context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SID, null),
        )
    }

    @Test
    fun migratesLegacySessionFileAndDeletesIt() {
        val legacyFile = context.dataStoreFile(LEGACY_SESSION_FILE)
        legacyFile.writeText("legacy-session-file")

        val migrated = EncryptedSessionCookieStore(context)

        assertEquals("legacy-session-file", migrated.sid())
        assertFalse(legacyFile.exists())
    }

    private companion object {
        const val LEGACY_PREFERENCES_NAME = "session_cookies"
        const val LEGACY_SESSION_FILE = "session.json"
        const val KEY_SID = "sid"
    }
}
