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
import com.wasylews.mykompot.domain.model.Profile
import com.wasylews.mykompot.domain.model.TariffPlan
import java.time.LocalDate

internal val previewAllGoodState = HomeUiState(
    content = HomeContent.Content(
        account = AccountStatus(
            state = AccountStatus.State.ALL_GOOD,
            profile = Profile(
                isActive = true,
                nextPaymentDate = LocalDate.of(2026, 11, 1),
                tariffPlan = TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
            ),
            daysLeft = 20,
        ),
        status = R.string.status_all_good,
        message = R.string.status_all_good_message,
        nextChargeLabel = R.string.next_charge,
    ),
)

internal fun previewPaymentDueState(): HomeUiState {
    val allGood = previewAllGoodState.content as HomeContent.Content
    return HomeUiState(
        content = allGood.copy(
            account = AccountStatus(
                state = AccountStatus.State.PAYMENT_DUE,
                profile = allGood.account.profile,
                daysLeft = 2,
            ),
            status = R.string.status_payment_due,
            message = R.string.status_payment_due_message,
            nextChargeLabel = R.string.due,
        ),
    )
}

internal val previewSuspendedState = HomeUiState(
    content = HomeContent.Content(
        account = AccountStatus(
            state = AccountStatus.State.SUSPENDED,
            profile = Profile(
                isActive = false,
                nextPaymentDate = null,
                tariffPlan = TariffPlan(id = "300GRN_100mb", name = "Standart 100", speed = 100, price = 300),
            ),
            daysLeft = null,
        ),
        status = R.string.status_suspended,
        message = R.string.status_suspended_message,
        nextChargeLabel = null,
    ),
)

/** Renders one preview per [HomeUiState] variant. */
class HomeUiStatePreviewParameterProvider : PreviewParameterProvider<HomeUiState> {
    private val states = listOf(
        previewAllGoodState,
        previewPaymentDueState(),
        previewSuspendedState,
        HomeUiState(content = HomeContent.Loading),
        HomeUiState(content = HomeContent.Unknown),
    )

    override val values: Sequence<HomeUiState>
        get() = states.asSequence()

    override fun getDisplayName(index: Int): String = when (val content = states[index].content) {
        is HomeContent.Content -> when (content.account.state) {
            AccountStatus.State.ALL_GOOD -> "All good"
            AccountStatus.State.PAYMENT_DUE -> "Payment due"
            AccountStatus.State.SUSPENDED -> "Suspended"
        }

        HomeContent.Loading -> "Loading"

        HomeContent.Unknown -> "Unknown"
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
