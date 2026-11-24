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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** JSON serializer for a nullable DataStore value, with a stable corruption message. */
@OptIn(ExperimentalSerializationApi::class)
abstract class JsonEntitySerializer<T : Any>(
    elementSerializer: KSerializer<T>,
    private val corruptionMessage: String,
) : Serializer<T?> {
    private val serializer = elementSerializer.nullable
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: T? = null

    override suspend fun readFrom(input: InputStream): T? = try {
        json.decodeFromString(serializer, input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException(corruptionMessage, e)
    }

    override suspend fun writeTo(
        t: T?,
        output: OutputStream,
    ) {
        @Suppress("BlockingMethodInNonBlockingContext") // DataStore calls this on its IO scope
        output.write(json.encodeToString(serializer, t).encodeToByteArray())
    }
}
