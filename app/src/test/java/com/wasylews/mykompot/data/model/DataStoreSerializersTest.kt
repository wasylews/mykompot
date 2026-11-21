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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class DataStoreSerializersTest {
    private val tariffPlan = TariffPlanEntity(
        id = "300GRN_100mb",
        name = "Standart 100",
        speed = 100,
        price = 300,
    )

    @Test
    fun `profile serializer round-trips a stored profile`() = runTest {
        val value = ProfileEntity(isActive = false, nextPaymentDate = null, tariffPlan = tariffPlan)

        val output = ByteArrayOutputStream()
        ProfileEntitySerializer.writeTo(value, output)

        assertEquals(value, ProfileEntitySerializer.readFrom(output.toByteArray().inputStream()))
    }

    @Test
    fun `profile serializer ignores unknown keys`() = runTest {
        val json =
            """
            {
              "isActive": true,
              "nextPaymentDate": "2026-10-01",
              "tariffPlan": {"id": "300GRN_100mb", "name": "Standart 100", "speed": 100, "price": 300},
              "futureField": "ignored"
            }
            """.trimIndent()

        assertEquals(
            ProfileEntity(isActive = true, nextPaymentDate = "2026-10-01", tariffPlan = tariffPlan),
            ProfileEntitySerializer.readFrom(json.byteInputStream()),
        )
    }

    @Test
    fun `profile serializer reports malformed data as corruption`() = runTest {
        assertCorrupt(ProfileEntitySerializer, "{not json", "Unable to read the stored profile.")
        assertCorrupt(ProfileEntitySerializer, """{"isActive": "yes"}""", "Unable to read the stored profile.")
    }

    @Test
    fun `tariff plans serializer round-trips a stored catalog`() = runTest {
        val value = TariffPlansEntity(
            plans = listOf(tariffPlan),
            etag = "etag-1",
            lastModified = "Thu, 01 Oct 2026 00:00:00 GMT",
        )

        val output = ByteArrayOutputStream()
        TariffPlansSerializer.writeTo(value, output)

        assertEquals(value, TariffPlansSerializer.readFrom(output.toByteArray().inputStream()))
    }

    @Test
    fun `tariff plans serializer ignores unknown keys`() = runTest {
        val json = """{"plans": [], "futureField": true}"""

        assertEquals(
            TariffPlansEntity(plans = emptyList()),
            TariffPlansSerializer.readFrom(json.byteInputStream()),
        )
    }

    @Test
    fun `tariff plans serializer reports malformed data as corruption`() = runTest {
        assertCorrupt(TariffPlansSerializer, "{not json", "Unable to read the stored tariff plans.")
        assertCorrupt(TariffPlansSerializer, """{"plans": "not-a-list"}""", "Unable to read the stored tariff plans.")
    }

    @Test
    fun `payments serializer round-trips stored payments`() = runTest {
        val value = PaymentsEntity(
            items = listOf(
                PaymentEntity(
                    dateTime = "2026-10-01T10:05:08",
                    amount = 300,
                    tariffName = "Standart 100",
                    source = "PRIVAT24",
                ),
            ),
            hasMore = true,
            nextPageOffset = 25,
        )

        val output = ByteArrayOutputStream()
        PaymentsSerializer.writeTo(value, output)

        assertEquals(value, PaymentsSerializer.readFrom(output.toByteArray().inputStream()))
    }

    @Test
    fun `payments serializer ignores unknown keys`() = runTest {
        val json = """{"items": [], "hasMore": false, "futureField": true}"""

        assertEquals(
            PaymentsEntity(items = emptyList(), hasMore = false),
            PaymentsSerializer.readFrom(json.byteInputStream()),
        )
    }

    @Test
    fun `payments serializer reports malformed data as corruption`() = runTest {
        assertCorrupt(PaymentsSerializer, "{not json", "Unable to read the stored payments.")
        assertCorrupt(PaymentsSerializer, """{"items": {}, "hasMore": true}""", "Unable to read the stored payments.")
    }

    @Test
    fun `payment due ledger serializer round-trips notified keys`() = runTest {
        val value = PaymentDueNotifiedEntity(notifiedKeys = listOf("DUE_DAY|2026-10-01"))

        val output = ByteArrayOutputStream()
        PaymentDueNotifiedSerializer.writeTo(value, output)

        assertEquals(value, PaymentDueNotifiedSerializer.readFrom(output.toByteArray().inputStream()))
    }

    @Test
    fun `payment due ledger serializer ignores unknown keys`() = runTest {
        val json = """{"notifiedKeys": [], "futureField": true}"""

        assertEquals(
            PaymentDueNotifiedEntity(notifiedKeys = emptyList()),
            PaymentDueNotifiedSerializer.readFrom(json.byteInputStream()),
        )
    }

    @Test
    fun `payment due ledger serializer reports malformed data as corruption`() = runTest {
        assertCorrupt(PaymentDueNotifiedSerializer, "{not json", "Unable to read payment reminder ledger.")
        assertCorrupt(
            PaymentDueNotifiedSerializer,
            """{"notifiedKeys": 42}""",
            "Unable to read payment reminder ledger.",
        )
    }

    private suspend fun <T> assertCorrupt(
        serializer: Serializer<T?>,
        json: String,
        expectedMessage: String,
    ) {
        var thrown: CorruptionException? = null
        try {
            serializer.readFrom(json.byteInputStream())
        } catch (e: CorruptionException) {
            thrown = e
        }

        val exception = requireNotNull(thrown) { "Expected CorruptionException for $json" }
        assertEquals(expectedMessage, exception.message)
        assertTrue(exception.cause is SerializationException)
    }
}
