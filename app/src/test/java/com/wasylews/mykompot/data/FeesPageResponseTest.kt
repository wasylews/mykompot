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

package com.wasylews.mykompot.data

import com.wasylews.mykompot.HtmlFixtures
import dev.burnoo.kspoon.Kspoon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeesPageResponseTest {
    @Test
    fun `parsing fees page`() {
        val page = HtmlFixtures.fees()
        val result = Kspoon.parse<FeesPageResponse>(page)

        assertEquals(25, result.items.size)
        assertEquals(
            ParsedFeeItem(
                date = "2026-10-01 10:05:08",
                description = "abonplata_privatbank",
                sum = "300",
            ),
            result.items.first(),
        )
        assertEquals(
            ParsedFeeItem(
                date = "2024-10-15 09:39:29",
                description = "abonplata_easypay",
                sum = "250",
            ),
            result.items.last(),
        )
    }

    @Test
    fun `parsing fees page 2`() {
        val page = HtmlFixtures.feesPage2()
        val result = Kspoon.parse<FeesPageResponse>(page)

        assertEquals(25, result.items.size)
        assertEquals(
            ParsedFeeItem(
                date = "2024-10-15 09:39:29",
                description = "abonplata_easypay",
                sum = "250",
            ),
            result.items.first(),
        )
        assertEquals(
            ParsedFeeItem(
                date = "2022-10-05 00:00:00",
                description = "Internet: 200GRN_100mb (35)(2022-10-01-2022-10-31)",
                sum = "200",
            ),
            result.items.last(),
        )
    }

    @Test
    fun `parses paginator and total on the first page`() {
        val result = Kspoon.parse<FeesPageResponse>(HtmlFixtures.fees())

        assertEquals(1, result.currentOffset)
        assertEquals(listOf(25, 50, 75, 100), result.pageOffsets.map { it.toInt() }.distinct())
        assertEquals(120, result.totalCount)
        assertEquals(25, result.nextPageOffset)
    }

    @Test
    fun `parses paginator and total on a later page`() {
        val result = Kspoon.parse<FeesPageResponse>(HtmlFixtures.feesPage2())

        assertEquals(25, result.currentOffset)
        assertEquals(listOf(1, 50, 75, 100), result.pageOffsets.map { it.toInt() }.distinct())
        assertEquals(121, result.totalCount)
        assertEquals(50, result.nextPageOffset)
    }

    @Test
    fun `missing paginator has no derived paging data`() {
        val result = Kspoon.parse<FeesPageResponse>("<html><body></body></html>")

        assertNull(result.currentOffset)
        assertNull(result.totalCount)
        assertNull(result.nextPageOffset)
    }
}
