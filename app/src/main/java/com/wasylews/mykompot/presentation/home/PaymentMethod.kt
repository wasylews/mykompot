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

package com.wasylews.mykompot.presentation.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

data class PaymentMethod(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    val packageName: String?,
    @param:StringRes val fallbackUrlRes: Int,
)

val PaymentMethodIconRes = SemanticsPropertyKey<Int>("PaymentMethodIconRes")
var SemanticsPropertyReceiver.paymentMethodIconRes by PaymentMethodIconRes

val paymentMethods = listOf(
    PaymentMethod(
        iconRes = com.wasylews.mykompot.R.drawable.ic_payment_privat24,
        titleRes = com.wasylews.mykompot.R.string.payment_method_privat24,
        packageName = null,
        fallbackUrlRes = com.wasylews.mykompot.R.string.payment_method_privat24_url,
    ),
    PaymentMethod(
        iconRes = com.wasylews.mykompot.R.drawable.ic_payment_monobank,
        titleRes = com.wasylews.mykompot.R.string.payment_method_monobank,
        packageName = "com.ftband.mono",
        fallbackUrlRes = com.wasylews.mykompot.R.string.payment_method_monobank_url,
    ),
    PaymentMethod(
        iconRes = com.wasylews.mykompot.R.drawable.ic_payment_easypay,
        titleRes = com.wasylews.mykompot.R.string.payment_method_easypay,
        packageName = "ua.easypay.clientandroid",
        fallbackUrlRes = com.wasylews.mykompot.R.string.payment_method_easypay_url,
    ),
)
