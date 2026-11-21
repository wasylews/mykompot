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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wasylews.mykompot.domain.model.TariffPlan
import org.junit.Test

class TariffPlansSheetTest : BaseAcceptanceTest() {
    @Test
    fun plansRequestDoesNotSendSessionCookie() {
        // GIVEN a stored session and a tariff catalog
        givenStoredSession()
        givenStoredTariffPlans()
        givenServerReturnsProfile()

        launch().use {
            assertShows(R.string.status_all_good)

            // WHEN I open the tariff plans
            openTariffPlans()

            // THEN the public plans request does not carry the portal session cookie
            assertPlansRequestHasNoSessionCookie()
        }
    }

    @Test
    fun showsTariffPlansSheet() {
        val plans = ProfileFixtures.tariffPlans()

        // GIVEN a stored session, tariff catalog, and profile from server
        givenStoredSession()
        givenStoredTariffPlans(plans)
        givenServerReturnsProfile()

        launch().use {
            assertShows(R.string.status_all_good)

            // WHEN I open the tariff plans
            openTariffPlans()

            // THEN I see the tariffs sheet with 3 plans and the selected one indicated
            composeRule.onNodeWithText(stringRes(R.string.tariffs)).assertIsDisplayed()
            assertShowsTariffPlans(plans = plans, selectedPlanId = "300GRN_100mb")
        }
    }

    @Test
    fun showsCachedTariffPlansWhenRefreshFails() {
        val plans = ProfileFixtures.tariffPlans()

        // GIVEN a stored session, cached tariff catalog, and the plans refresh fails
        givenStoredSession()
        givenStoredTariffPlans(plans)
        givenHomeResponses(plansStatus = 502)

        launch().use {
            assertShows(R.string.status_all_good)

            // WHEN I open the tariff plans
            openTariffPlans()

            // THEN I see the cached tariff plans
            composeRule.onNodeWithText(stringRes(R.string.tariffs)).assertIsDisplayed()
            assertShowsTariffPlans(plans = plans, selectedPlanId = "300GRN_100mb")
        }
    }

    @Test
    fun showsErrorWhenTariffsUnavailableAndNoneStored() {
        // GIVEN a stored session with no cached tariff plans and the plans refresh fails
        givenStoredSession()
        givenHomeResponses(plansStatus = 502)

        launch().use {
            assertShows(R.string.status_all_good)

            // WHEN I open the tariff plans
            openTariffPlans()

            // THEN I see an error state with retry
            composeRule.onNodeWithText(stringRes(R.string.tariffs)).assertIsDisplayed()
            composeRule.onNodeWithText(stringRes(R.string.tariffs_load_error)).assertIsDisplayed()
            composeRule.onNodeWithText(stringRes(R.string.retry)).assertIsDisplayed()
        }
    }

    @Test
    fun retriesLoadingTariffPlans() {
        val plans = ProfileFixtures.tariffPlans()

        // GIVEN a stored session with no cached tariff plans and the plans refresh fails
        givenStoredSession()
        givenHomeResponses(plansStatus = 502)

        launch().use {
            assertShows(R.string.status_all_good)
            openTariffPlans()
            composeRule.onNodeWithText(stringRes(R.string.tariffs_load_error)).assertIsDisplayed()

            // WHEN the server returns tariffs and I retry
            givenHomeResponses()
            composeRule.onNodeWithText(stringRes(R.string.retry)).performClick()

            // THEN I see the tariff plans
            assertShowsTariffPlans(plans = plans, selectedPlanId = "300GRN_100mb")
        }
    }

    private fun openTariffPlans() {
        composeRule.onNodeWithText(stringRes(R.string.tariff)).performClick()
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
