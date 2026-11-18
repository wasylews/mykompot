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
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Clock

/** Observes the derived [AccountStatus] for the cached profile. */
fun interface ObserveStatus {
    fun statusFlow(): Flow<AccountStatus?>
}

/**
 * Derives [AccountStatus] from the cached [com.wasylews.mykompot.domain.model.Profile]:
 * inactive accounts are [AccountStatus.State.SUSPENDED]; active accounts with a next payment due
 * within [PAYMENT_DUE_WINDOW_DAYS] days (including today/overdue) are
 * [AccountStatus.State.PAYMENT_DUE]; others are [AccountStatus.State.ALL_GOOD].
 */
@Factory
class ObserveStatusUseCase(
    private val repository: PortalRepository,
    private val clock: Clock,
) : ObserveStatus {
    override fun statusFlow(): Flow<AccountStatus?> = repository.profileFlow()
        .map { profile -> profile?.toAccountStatus(clock.today()) }

    private fun Clock.today(): LocalDate = Instant
        .ofEpochMilli(now().toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    private fun Profile.toAccountStatus(today: LocalDate): AccountStatus {
        val daysLeft = nextPaymentDate?.let { ChronoUnit.DAYS.between(today, it) }
        val state = when {
            !isActive -> AccountStatus.State.SUSPENDED
            daysLeft != null && daysLeft <= PAYMENT_DUE_WINDOW_DAYS -> AccountStatus.State.PAYMENT_DUE
            else -> AccountStatus.State.ALL_GOOD
        }
        return AccountStatus(state = state, profile = this, daysLeft = daysLeft)
    }

    private companion object {
        const val PAYMENT_DUE_WINDOW_DAYS = 3L
    }
}
