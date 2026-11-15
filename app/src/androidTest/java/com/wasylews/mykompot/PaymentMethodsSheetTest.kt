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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.intent.rule.IntentsRule
import com.wasylews.mykompot.presentation.home.PaymentMethod
import com.wasylews.mykompot.presentation.home.PaymentMethodIconRes
import com.wasylews.mykompot.presentation.home.paymentMethods
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
        // GIVEN a saved session with a payment-due profile and the payment methods sheet open
        givenPaymentDueHome()
        stubExternalIntents()

        launch().use {
            openPaymentMethods()

            // WHEN I tap Privat24
            composeRule
                .onNodeWithText(stringRes(R.string.payment_method_privat24))
                .performClick()

            // THEN it opens the Privat24 payment URL
            intended(
                allOf(
                    hasAction(Intent.ACTION_VIEW),
                    hasData(stringRes(R.string.payment_method_privat24_url)),
                ),
            )
        }
    }

    @Test
    fun opensMonobank() {
        // GIVEN a saved session with a payment-due profile and the payment methods sheet open
        givenPaymentDueHome()
        stubExternalIntents()

        launch().use {
            openPaymentMethods()

            // WHEN I tap monobank
            composeRule
                .onNodeWithText(stringRes(R.string.payment_method_monobank))
                .performClick()

            // THEN it opens the monobank fallback URL
            intended(
                allOf(
                    hasAction(Intent.ACTION_VIEW),
                    hasData(stringRes(R.string.payment_method_monobank_url)),
                ),
            )
        }
    }

    @Test
    fun opensEasyPay() {
        // GIVEN a saved session with a payment-due profile and the payment methods sheet open
        givenPaymentDueHome()
        stubExternalIntents()

        launch().use {
            openPaymentMethods()

            // WHEN I tap EasyPay
            composeRule
                .onNodeWithText(stringRes(R.string.payment_method_easypay))
                .performClick()

            // THEN it opens the EasyPay fallback URL
            intended(
                allOf(
                    hasAction(Intent.ACTION_VIEW),
                    hasData(stringRes(R.string.payment_method_easypay_url)),
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
            composeRule.onNodeWithText(stringRes(method.titleRes)).assertIsDisplayed()
            composeRule.onNode(hasPaymentMethodIcon(method.iconRes)).assertIsDisplayed()
        }
    }

    private fun stubExternalIntents() {
        intending(not(isInternal())).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null),
        )
    }
}
