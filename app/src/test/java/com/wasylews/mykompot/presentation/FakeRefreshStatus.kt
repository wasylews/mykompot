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

import com.wasylews.mykompot.domain.RefreshStatus
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.domain.model.TariffPlan
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDate

class FakeRefreshStatus(
    var result: RefreshResult = RefreshResult.Updated(
        Profile(
            isActive = true,
            nextPaymentDate = LocalDate.of(2026, 10, 27),
            tariffPlan = TariffPlan(id = "300GRN_100mb", name = "Standart", speed = 100, price = 300),
        ),
    ),
) : RefreshStatus {
    var calls = 0
    private var gate: CompletableDeferred<Unit>? = null

    fun pause() {
        gate = CompletableDeferred()
    }

    fun resume() {
        gate?.complete(Unit)
        gate = null
    }

    override suspend fun invoke(): RefreshResult {
        calls++
        gate?.await()
        return result
    }
}
