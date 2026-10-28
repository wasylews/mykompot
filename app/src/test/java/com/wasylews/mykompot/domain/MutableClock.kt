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

import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class MutableClock(
    date: LocalDate,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : Clock {
    private var current: Instant = Instant.fromEpochMilliseconds(date.atStartOfDay(zone).toInstant().toEpochMilli())

    override fun now(): Instant = current

    fun set(date: LocalDate) {
        current = Instant.fromEpochMilliseconds(date.atStartOfDay(zone).toInstant().toEpochMilli())
    }

    fun advanceBy(duration: Duration) {
        current = current.plus(duration)
    }
}
