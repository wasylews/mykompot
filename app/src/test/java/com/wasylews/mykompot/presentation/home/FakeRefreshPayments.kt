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

package com.wasylews.mykompot.presentation.home

import com.wasylews.mykompot.domain.RefreshPayments
import com.wasylews.mykompot.domain.model.PaymentsActionResult
import kotlinx.coroutines.CompletableDeferred

class FakeRefreshPayments : RefreshPayments {
    var calls = 0
    var result: PaymentsActionResult = PaymentsActionResult.Success
    private val gates = mutableListOf<CompletableDeferred<Unit>>()

    /** Blocks the next call until its gate is resumed. */
    fun pause() {
        gates.add(CompletableDeferred())
    }

    /** Releases the gate for a 1-based [call] number, defaulting to the first call. */
    fun resume(call: Int = 1) {
        gates.getOrNull(call - 1)?.complete(Unit)
    }

    override suspend fun invoke(): PaymentsActionResult {
        val call = ++calls
        gates.getOrNull(call - 1)?.await()
        return result
    }
}
