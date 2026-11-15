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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wasylews.mykompot.domain.model.TariffPlan
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TariffPlansSheetTest : BaseAcceptanceTest() {
    @Test
    fun showsTariffPlansSheet() {
        val plans = listOf(
            TariffPlan(id = "260GRN_50mb", name = "Econom 50", speed = 50, price = 260),
            TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
            TariffPlan(id = "350GRN_300mb", name = "LUX 300", speed = 300, price = 350),
        )

        // GIVEN a stored session, tariff catalog, and profile from server
        givenStoredSession()
        givenStoredTariffPlans(plans)
        givenServerReturnsProfile()

        launch().use {
            assertShows(R.string.status_all_good)
            composeRule.onNodeWithText("Standart 100").assertIsDisplayed()

            // WHEN I open the tariff plans
            composeRule.onAllNodesWithText("Standart 100")[0].performClick()

            // THEN I see the tariffs sheet with 3 plans and the selected one indicated
            composeRule.onNodeWithText("Tariffs").assertIsDisplayed()
            assertShowsTariffPlans(plans = plans, selectedPlanId = "300GRN_100mb")
        }
    }

    private fun assertShowsTariffPlans(
        plans: List<TariffPlan>,
        selectedPlanId: String,
    ) {
        plans.forEach { plan ->
            composeRule.onAllNodesWithText(plan.name, substring = true)[0].assertIsDisplayed()
            composeRule
                .onAllNodesWithText(stringRes(R.string.tariff_speed, plan.speed), substring = true)[0]
                .assertIsDisplayed()
            composeRule
                .onAllNodesWithText(plan.price.toString(), substring = true)[0]
                .assertIsDisplayed()
        }
        plans.first { it.id == selectedPlanId }
        composeRule
            .onNodeWithText(stringRes(R.string.your_plan), substring = true)
            .assertIsDisplayed()
    }
}
