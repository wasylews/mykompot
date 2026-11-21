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

package com.wasylews.mykompot.data.model

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object PaymentDueNotifiedSerializer : Serializer<PaymentDueNotifiedEntity?> {
    override val defaultValue: PaymentDueNotifiedEntity? = null

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(input: InputStream): PaymentDueNotifiedEntity? = try {
        json.decodeFromString<PaymentDueNotifiedEntity?>(input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unable to read payment reminder ledger.", e)
    }

    override suspend fun writeTo(
        t: PaymentDueNotifiedEntity?,
        output: OutputStream,
    ) {
        @Suppress("BlockingMethodInNonBlockingContext") // DataStore calls this on its IO scope
        output.write(json.encodeToString<PaymentDueNotifiedEntity?>(t).encodeToByteArray())
    }
}
