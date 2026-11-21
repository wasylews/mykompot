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
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

/** [SessionCookieStore] backed by Keystore-encrypted preferences. */
@Singleton
class EncryptedSessionCookieStore(
    context: Context,
) : SessionCookieStore {
    private val appContext = context.applicationContext
    private val preferences = createEncryptedPreferences(
        appContext,
        PREFERENCES_NAME,
        "encrypted session cookies",
    )
    private val sid = MutableStateFlow(preferences.getString(KEY_SID, null))

    init {
        if (sid.value == null) {
            legacySid()?.let(::saveSid)
        }
    }

    override fun sid(): String? = sid.value

    override fun saveSid(sid: String) {
        preferences.edit { putString(KEY_SID, sid) }
        this.sid.value = sid
    }

    override fun clear() {
        preferences.edit { remove(KEY_SID) }
        sid.value = null
    }

    override fun sidFlow(): Flow<String?> = sid.asStateFlow()

    private fun legacySid(): String? {
        val legacyPreferences = appContext.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
        legacyPreferences.getString(KEY_SID, null)
            ?.takeIf(String::isNotBlank)
            ?.let {
                legacyPreferences.edit { remove(KEY_SID) }
                return it
            }

        val legacyFile = appContext.dataStoreFile(LEGACY_SESSION_FILE)
        if (!legacyFile.exists()) return null
        val storedSid = runCatching { legacyFile.readText().trim().ifBlank { null } }.getOrNull()
        legacyFile.delete()
        return storedSid
    }

    companion object {
        const val PREFERENCES_NAME = "session_cookies_encrypted"
        private const val LEGACY_PREFERENCES_NAME = "session_cookies"
        private const val LEGACY_SESSION_FILE = "session.json"
        private const val KEY_SID = "sid"
    }
}
