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

package com.wasylews.mykompot

import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A [Clock] whose time tests can set or advance explicitly. */
class MutableTestClock(
    private var current: Instant,
) : Clock {
    override fun now(): Instant = current

    fun set(instant: Instant) {
        current = instant
    }

    fun advanceBy(duration: Duration) {
        current = current.plus(duration)
    }

    companion object {
        /** Creates a clock at the start of [date] in [zone]. */
        fun atStartOfDay(
            date: LocalDate,
            zone: ZoneId = ZoneId.systemDefault(),
        ): MutableTestClock {
            val epochMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
            return MutableTestClock(Instant.fromEpochMilliseconds(epochMillis))
        }
    }
}
