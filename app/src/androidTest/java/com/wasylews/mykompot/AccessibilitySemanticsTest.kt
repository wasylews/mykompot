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

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.wasylews.mykompot.presentation.home.StatusCard
import com.wasylews.mykompot.presentation.home.previewAllGoodState
import com.wasylews.mykompot.presentation.login.LoginScreen
import com.wasylews.mykompot.presentation.login.LoginUiState
import com.wasylews.mykompot.presentation.payments.PaymentMethod
import com.wasylews.mykompot.presentation.payments.PaymentMethodIconRes
import com.wasylews.mykompot.presentation.payments.PaymentMethodsContent
import com.wasylews.mykompot.presentation.payments.paymentMethods
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessibilitySemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statusChipUsesNaturalCaseForAccessibility() {
        composeRule.setContent {
            MyKompotTheme {
                StatusCard(state = previewAllGoodState)
            }
        }

        composeRule
            .onNode(hasContentDescription(string(R.string.status_all_good)))
            .assertIsDisplayed()
        composeRule
            .onNode(hasText(string(R.string.status_all_good).uppercase()))
            .assertIsDisplayed()
    }

    @Test
    fun nextChargeSemanticsAreMerged() {
        composeRule.setContent {
            MyKompotTheme {
                StatusCard(state = previewAllGoodState)
            }
        }

        composeRule
            .onNode(
                hasText(string(R.string.next_charge), ignoreCase = true)
                    .and(hasText("in 20 days", substring = true)),
            )
            .assertIsDisplayed()
    }

    @Test
    fun tariffRowExposesButtonSemantics() {
        var clicks = 0
        composeRule.setContent {
            MyKompotTheme {
                StatusCard(
                    state = previewAllGoodState,
                    onTariffClick = { clicks++ },
                )
            }
        }

        val row = composeRule.onNode(
            hasButtonRole()
                .and(hasText(string(R.string.tariff)))
                .and(hasClickLabel(string(R.string.view_tariff_plans))),
        )
        row.assertHasClickAction()
        row.assertHeightIsAtLeast(48.dp)
        row.performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun paymentMethodRowsExposeButtonSemanticsAndDecorativeIcons() {
        var clicked: PaymentMethod? = null
        composeRule.setContent {
            MyKompotTheme {
                PaymentMethodsContent(
                    methods = paymentMethods,
                    onMethodClick = { clicked = it },
                )
            }
        }

        val method = paymentMethods.first()
        val row = composeRule.onNode(
            hasButtonRole()
                .and(hasText(string(method.titleRes)))
                .and(hasClickLabel(string(R.string.open_payment_method))),
        )
        row.assertHasClickAction()
        row.assertHeightIsAtLeast(48.dp)
        row.performClick()

        assertEquals(method, clicked)
        assertIconHasNoContentDescription(method.iconRes)
    }

    @Test
    fun loginButtonHasAccessibleName() {
        assertLoginButtonDescription(isLoading = false, label = string(R.string.log_in))
    }

    @Test
    fun loginButtonKeepsAccessibleNameWhileLoading() {
        assertLoginButtonDescription(isLoading = true, label = string(R.string.logging_in))
    }

    private fun assertLoginButtonDescription(
        isLoading: Boolean,
        label: String,
    ) {
        composeRule.setContent {
            MyKompotTheme {
                LoginScreen(
                    uiState = LoginUiState(
                        usernameState = TextFieldState(""),
                        passwordState = TextFieldState(""),
                        isLoading = isLoading,
                    ),
                    onLoginClick = {},
                    onRememberMeChange = {},
                    onErrorConsumed = {},
                )
            }
        }

        composeRule
            .onNode(hasButtonRole().and(hasContentDescription(label)))
            .assertIsDisplayed()
    }

    private fun assertIconHasNoContentDescription(iconRes: Int) {
        val icon = composeRule.onNode(
            SemanticsMatcher.expectValue(PaymentMethodIconRes, iconRes),
            useUnmergedTree = true,
        ).fetchSemanticsNode()

        assertTrue(
            icon.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.isEmpty(),
        )
    }
}

private fun string(
    @StringRes res: Int,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(res)

private fun hasButtonRole(): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

private fun hasClickLabel(label: String): SemanticsMatcher = SemanticsMatcher("click label '$label'") { node ->
    node.config.contains(SemanticsActions.OnClick) && node.config[SemanticsActions.OnClick].label == label
}
