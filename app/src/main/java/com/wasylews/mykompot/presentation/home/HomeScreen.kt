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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.wasylews.mykompot.presentation.payments.PaymentMethod
import com.wasylews.mykompot.presentation.payments.PaymentMethodsBottomSheet
import com.wasylews.mykompot.presentation.payments.PaymentMethodsUiState
import com.wasylews.mykompot.presentation.payments.paymentMethods
import com.wasylews.mykompot.presentation.tariffs.TariffPlansBottomSheet
import com.wasylews.mykompot.presentation.tariffs.TariffPlansUiState
import com.wasylews.mykompot.ui.theme.BackgroundDark
import com.wasylews.mykompot.ui.theme.BackgroundLight
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.ui.theme.Spacing

internal const val HOME_SCREEN_TEST_TAG = "home_screen"
internal const val HOME_LOADING_TEST_TAG = "home_loading"
internal const val HOME_LIST_TEST_TAG = "home_list"
internal const val HOME_REFRESH_INDICATOR_TEST_TAG = "home_refresh_indicator"

private const val HOME_STATUS_KEY = "home_status"

/** User actions exposed by the home screen. */
data class HomeActions(
    val onPullToRefresh: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onPayClick: () -> Unit = {},
    val onTariffClick: () -> Unit = {},
    val onTariffRetry: () -> Unit = {},
    val onTariffSheetDismiss: () -> Unit = {},
    val onPaymentMethodsDismiss: () -> Unit = {},
    val onPaymentMethodClick: (PaymentMethod) -> Unit = {},
    val onLoadMorePayments: () -> Unit = {},
    val onRetryLoadMorePayments: () -> Unit = {},
    val onRetryPayments: () -> Unit = {},
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    tariffPlansState: TariffPlansUiState,
    paymentMethodsState: PaymentMethodsUiState,
    modifier: Modifier = Modifier,
    paymentsState: PaymentsUiState = PaymentsUiState(),
    actions: HomeActions = HomeActions(),
) {
    Box(modifier = modifier) {
        HomeContent(
            state = state,
            paymentsState = paymentsState,
            actions = actions,
            modifier = Modifier.fillMaxSize(),
        )

        if (tariffPlansState.isVisible) {
            TariffPlansBottomSheet(
                state = tariffPlansState,
                onDismissRequest = actions.onTariffSheetDismiss,
                onRetry = actions.onTariffRetry,
            )
        }

        if (paymentMethodsState.isVisible) {
            PaymentMethodsBottomSheet(
                methods = paymentMethods,
                onMethodClick = actions.onPaymentMethodClick,
                onDismissRequest = actions.onPaymentMethodsDismiss,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    paymentsState: PaymentsUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = actions.onPullToRefresh,
        state = pullToRefreshState,
        modifier = modifier.testTag(HOME_SCREEN_TEST_TAG),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(
                        if (state.isRefreshing) {
                            Modifier.testTag(HOME_REFRESH_INDICATOR_TEST_TAG)
                        } else {
                            Modifier
                        },
                    ),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(HOME_LIST_TEST_TAG),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
        ) {
            stickyHeader(key = HOME_STATUS_KEY) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(bottom = Spacing.md),
                ) {
                    StatusCard(
                        state = state,
                        onPayClick = actions.onPayClick,
                        onTariffClick = actions.onTariffClick,
                        onRetry = actions.onRetry,
                    )
                }
            }
            paymentsSection(
                paymentsState = paymentsState,
                onLoadMorePayments = actions.onLoadMorePayments,
                onRetryLoadMorePayments = actions.onRetryLoadMorePayments,
                onRetry = actions.onRetryPayments,
            )
        }
    }
}

@Preview(name = "States (Light | Dark)", showBackground = true, widthDp = 800)
@Composable
fun HomeScreenStatesPreview(
    @PreviewParameter(HomeUiStatePreviewParameterProvider::class) state: HomeUiState,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(false to BackgroundLight, true to BackgroundDark).forEach { (dark, bg) ->
            MyKompotTheme(darkTheme = dark, dynamicColor = false) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.weight(1f),
                    color = bg,
                ) {
                    HomeScreen(
                        state = state,
                        tariffPlansState = TariffPlansUiState(),
                        paymentMethodsState = PaymentMethodsUiState(),
                        paymentsState = samplePaymentsState(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
