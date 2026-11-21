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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wasylews.mykompot.domain.model.Credentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedCredentialsStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = EncryptedCredentialsStore(context)

    @Before
    fun setUp() {
        store.clear()
    }

    @Test
    fun readsSavedCredentials() {
        val credentials = Credentials("user", "pass")

        store.save(credentials)

        assertEquals(credentials, store.read())
        assertTrue(store.hasCredentials())
    }

    @Test
    fun clearsCredentials() {
        store.save(Credentials("user", "pass"))

        store.clear()

        assertNull(store.read())
        assertFalse(store.hasCredentials())
    }

    @Test
    fun doesNotStorePlaintextCredentials() {
        store.save(Credentials("secret-user", "secret-password"))

        val rawValues = context
            .getSharedPreferences(EncryptedCredentialsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .all
            .values
            .joinToString(" ")

        assertFalse("username leaked: $rawValues", "secret-user" in rawValues)
        assertFalse("password leaked: $rawValues", "secret-password" in rawValues)
    }
}
