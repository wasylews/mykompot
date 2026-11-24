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

package com.wasylews.mykompot.presentation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowKtxTest {
    @Test
    fun `upstream failure emits fallback and notifies`() = runTest {
        val failure = IOException("Broken storage")
        val errors = mutableListOf<Throwable>()

        val values = flow<Int> {
            emit(1)
            throw failure
        }
            .catchAsNull { errors += it }
            .toList()

        assertEquals(listOf(1, null), values)
        assertEquals(listOf(failure), errors)
    }

    @Test
    fun `cancellation emits no fallback and does not notify`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val values = mutableListOf<Int?>()
        val errors = mutableListOf<Throwable>()

        val job = launch {
            flow<Int> {
                emit(1)
                gate.await()
                emit(2)
            }
                .catchAsNull { errors += it }
                .collect { values += it }
        }
        runCurrent()
        assertEquals(listOf(1), values)

        job.cancelAndJoin()

        assertEquals(listOf(1), values)
        assertTrue(errors.isEmpty())
    }
}
