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
import com.wasylews.mykompot.presentation.profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataStoreLocalStorageTest {
    @Test
    fun `load saved profile`() = runTest {
        val dataStore = FakeDataStore()
        val store = DataStoreLocalStorage(dataStore)

        val profile = profile()
        store.saveProfile(profile)

        val savedProfile = store.loadProfile()
        assertEquals(profile, savedProfile)
    }

    @Test
    fun `load profile returns null when nothing is stored`() = runTest {
        val store = DataStoreLocalStorage(FakeDataStore())

        val savedProfile = store.loadProfile()

        assertNull(savedProfile)
    }
}

class FakeDataStore : DataStore<ProfileEntity?> {
    private val savedProfileFlow = MutableStateFlow<ProfileEntity?>(null)

    override val data: Flow<ProfileEntity?> = savedProfileFlow

    override suspend fun updateData(transform: suspend (t: ProfileEntity?) -> ProfileEntity?): ProfileEntity? {
        savedProfileFlow.update { transform(it) }
        return savedProfileFlow.value
    }
}
