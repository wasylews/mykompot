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

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.intent.rule.IntentsRule
import com.wasylews.mykompot.presentation.payments.PAYMENT_METHODS_SHEET_TEST_TAG
import com.wasylews.mykompot.presentation.payments.PaymentMethod
import com.wasylews.mykompot.presentation.payments.PaymentMethodIconRes
import com.wasylews.mykompot.presentation.payments.paymentMethods
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test

fun hasPaymentMethodIcon(
    @DrawableRes iconRes: Int,
): SemanticsMatcher = SemanticsMatcher.expectValue(PaymentMethodIconRes, iconRes)

class PaymentMethodsSheetTest : BaseAcceptanceTest() {
    @get:Rule
    val intentsRule = IntentsRule()

    @Test
    fun showsPaymentMethodsSheet() {
        // GIVEN a saved session with a payment-due profile
        givenPaymentDueHome()

        launch().use {
            // WHEN I open the payment methods
            openPaymentMethods()

            // THEN I see the payment methods sheet
            composeRule.onNodeWithText(stringRes(R.string.payment_methods)).assertIsDisplayed()
            assertShowsPaymentMethods(paymentMethods)
        }
    }

    @Test
    fun opensPrivat24() {
        assertOpensPaymentMethod(
            titleRes = R.string.payment_method_privat24,
            urlRes = R.string.payment_method_privat24_url,
        )
    }

    @Test
    fun opensMonobank() {
        assertOpensPaymentMethod(
            titleRes = R.string.payment_method_monobank,
            urlRes = R.string.payment_method_monobank_url,
        )
    }

    @Test
    fun opensEasyPay() {
        assertOpensPaymentMethod(
            titleRes = R.string.payment_method_easypay,
            urlRes = R.string.payment_method_easypay_url,
        )
    }

    private fun assertOpensPaymentMethod(
        @StringRes titleRes: Int,
        @StringRes urlRes: Int,
    ) {
        // GIVEN a saved session with a payment-due profile and the payment methods sheet open
        givenPaymentDueHome()
        stubExternalIntents()

        launch().use {
            openPaymentMethods()

            // WHEN I tap a payment method
            composeRule
                .onNode(
                    hasText(stringRes(titleRes)).and(hasAnyAncestor(hasTestTag(PAYMENT_METHODS_SHEET_TEST_TAG))),
                    useUnmergedTree = true,
                )
                .performClick()

            // THEN it opens the payment URL
            intended(
                allOf(
                    hasAction(Intent.ACTION_VIEW),
                    hasData(stringRes(urlRes)),
                ),
            )
        }
    }

    private fun openPaymentMethods() {
        assertShows(R.string.status_payment_due)
        assertShows(R.string.pay_now)
        composeRule.onNodeWithText(stringRes(R.string.pay_now)).performClick()
    }

    private fun assertShowsPaymentMethods(methods: List<PaymentMethod>) {
        methods.forEach { method ->
            composeRule
                .onNode(
                    hasText(stringRes(method.titleRes)).and(hasAnyAncestor(hasTestTag(PAYMENT_METHODS_SHEET_TEST_TAG))),
                    useUnmergedTree = true,
                )
                .assertIsDisplayed()
            composeRule
                .onNode(
                    hasPaymentMethodIcon(method.iconRes).and(hasAnyAncestor(hasTestTag(PAYMENT_METHODS_SHEET_TEST_TAG))),
                    useUnmergedTree = true,
                )
                .assertIsDisplayed()
        }
    }

    private fun stubExternalIntents() {
        intending(not(isInternal())).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null),
        )
    }
}
