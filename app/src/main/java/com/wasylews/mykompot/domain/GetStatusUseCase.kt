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

package com.wasylews.mykompot.domain

import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.domain.repository.PortalRepository
import org.koin.core.annotation.Factory
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Clock

interface GetStatus {
    suspend operator fun invoke(): AccountStatus
}

@Factory
class GetStatusUseCase(
    private val repository: PortalRepository,
    private val clock: Clock,
) : GetStatus {
    override suspend operator fun invoke(): AccountStatus {
        val profile = repository.loadProfile()
        val daysLeft = ChronoUnit.DAYS.between(clock.today(), profile.nextPaymentDate)
        val state = when {
            !profile.isActive -> AccountStatus.State.PAYMENT_DUE
            daysLeft <= PAYMENT_DUE_WINDOW_DAYS -> AccountStatus.State.PAYMENT_DUE
            else -> AccountStatus.State.ALL_GOOD
        }
        return AccountStatus(state, profile, daysLeft)
    }

    private fun Clock.today(): LocalDate = java.time.Instant
        .ofEpochMilli(now().toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    private companion object {
        const val PAYMENT_DUE_WINDOW_DAYS = 3L
    }
}
