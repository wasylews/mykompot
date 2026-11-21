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
import androidx.core.content.edit
import com.wasylews.mykompot.domain.model.Credentials
import com.wasylews.mykompot.domain.repository.CredentialsStore
import org.koin.core.annotation.Singleton

/** [CredentialsStore] backed by Keystore-encrypted preferences. */
@Singleton
class EncryptedCredentialsStore(
    context: Context,
) : CredentialsStore {
    private val preferences = createEncryptedPreferences(
        context.applicationContext,
        PREFERENCES_NAME,
        "encrypted credentials",
    )

    override fun save(credentials: Credentials) {
        preferences.edit {
            putString(KEY_USERNAME, credentials.username)
            putString(KEY_PASSWORD, credentials.password)
        }
    }

    override fun read(): Credentials? {
        val username = preferences.getString(KEY_USERNAME, null) ?: return null
        val password = preferences.getString(KEY_PASSWORD, null) ?: return null
        return Credentials(username, password)
    }

    override fun clear() {
        preferences.edit { clear() }
    }

    companion object {
        const val PREFERENCES_NAME = "credentials"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}
