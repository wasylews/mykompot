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
import com.wasylews.mykompot.data.model.PaymentDueNotifiedEntity
import com.wasylews.mykompot.domain.PaymentDueNotifiedStore
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
class DataStorePaymentDueNotifiedStore(
    @Named("paymentDueNotified") private val dataStore: DataStore<PaymentDueNotifiedEntity?>,
) : PaymentDueNotifiedStore {
    override suspend fun isNotified(key: String): Boolean = dataStore.data.first()?.notifiedKeys?.contains(key) == true

    override suspend fun markNotified(key: String) {
        dataStore.updateData { current ->
            val keys = current?.notifiedKeys.orEmpty()
            if (key in keys) {
                current
            } else {
                PaymentDueNotifiedEntity(keys + key)
            }
        }
    }

    override suspend fun clear() {
        dataStore.updateData { null }
    }
}
