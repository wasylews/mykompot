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

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.model.AccountStatus
import java.time.LocalDate

internal val previewAllGoodState = HomeUiState(
    accountState = AccountStatus.State.ALL_GOOD,
    status = R.string.status_all_good,
    message = R.string.status_all_good_message,
    nextChargeDate = LocalDate.of(2026, 11, 1),
    daysUntilPayment = 20,
    tariffName = "Standart 100",
    tariffPrice = 300,
)

internal fun previewPaymentDueState() = previewAllGoodState.copy(
    accountState = AccountStatus.State.PAYMENT_DUE,
    status = R.string.status_payment_due,
    message = R.string.status_payment_due_message,
    nextChargeLabel = R.string.due,
    daysUntilPayment = 2,
)

/** Renders one preview per [HomeUiState] variant. */
class HomeUiStatePreviewParameterProvider : PreviewParameterProvider<HomeUiState> {
    private val states = listOf(previewAllGoodState, previewPaymentDueState())

    override val values = states.asSequence()

    override fun getDisplayName(index: Int): String = when (states[index].accountState) {
        AccountStatus.State.ALL_GOOD -> "All good"
        AccountStatus.State.PAYMENT_DUE -> "Payment due"
        AccountStatus.State.UNKNOWN -> "Unknown"
    }
}

/** Renders every preview in light and dark mode. */
@Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = 0xFFF4F3FB,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF0E0D13,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
internal annotation class PreviewLightDark
