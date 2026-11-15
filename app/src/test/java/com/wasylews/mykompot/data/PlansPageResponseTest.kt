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
import org.junit.Test

class PlansPageResponseTest {
    @Test
    fun `parsing plans page`() {
        val page = HtmlFixtures.plans()
        val result = Kspoon.parse<PlansPageResponse>(page)

        assertEquals(
            listOf(
                ParsedPlanItem(name = "Econom 50", price = "260 грн./міс.", speed = 50),
                ParsedPlanItem(name = "Standart 100", price = "300 грн./міс.", speed = 100),
                ParsedPlanItem(name = "LUX 300", price = "350 грн./міс.", speed = 300),
                ParsedPlanItem(name = "Подобова оплата", price = "15 грн./доба", speed = 50),
            ),
            result.items,
        )
    }
}
