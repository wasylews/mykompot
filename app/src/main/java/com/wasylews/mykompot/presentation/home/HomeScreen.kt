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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.wasylews.mykompot.R
import com.wasylews.mykompot.presentation.payments.PaymentMethod
import com.wasylews.mykompot.presentation.payments.PaymentMethodsBottomSheet
import com.wasylews.mykompot.presentation.payments.PaymentMethodsUiState
import com.wasylews.mykompot.presentation.payments.paymentMethods
import com.wasylews.mykompot.presentation.tariffs.TariffPlansBottomSheet
import com.wasylews.mykompot.presentation.tariffs.TariffPlansUiState
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.ui.theme.Spacing

internal const val HOME_SCREEN_TEST_TAG = "home_screen"
internal const val HOME_LOADING_TEST_TAG = "home_loading"

@Composable
fun HomeScreen(
    state: HomeUiState,
    tariffPlansState: TariffPlansUiState,
    paymentMethodsState: PaymentMethodsUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onPayClick: () -> Unit = {},
    onTariffClick: () -> Unit = {},
    onTariffSheetDismiss: () -> Unit = {},
    onPaymentMethodsDismiss: () -> Unit = {},
    onPaymentMethodClick: (PaymentMethod) -> Unit = {},
    onErrorConsumed: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.errorId?.let { stringResource(it) }.orEmpty()

    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
            onErrorConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(HOME_SCREEN_TEST_TAG),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.your_internet),
                    style = MaterialTheme.typography.titleLarge,
                )
                StatusCard(
                    state = state,
                    onPayClick = onPayClick,
                    onTariffClick = onTariffClick,
                )
            }
        }

        if (tariffPlansState.isVisible) {
            TariffPlansBottomSheet(
                plans = tariffPlansState.plans,
                currentPlanIndex = tariffPlansState.selectedPlanIndex,
                onDismissRequest = onTariffSheetDismiss,
            )
        }

        if (paymentMethodsState.isVisible) {
            PaymentMethodsBottomSheet(
                methods = paymentMethods,
                onMethodClick = onPaymentMethodClick,
                onDismissRequest = onPaymentMethodsDismiss,
            )
        }
    }
}

@PreviewLightDark
@Composable
fun HomeScreenPreview(
    @PreviewParameter(HomeUiStatePreviewParameterProvider::class) state: HomeUiState,
) {
    MyKompotTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) { innerPadding ->
            HomeScreen(
                state = state,
                tariffPlansState = TariffPlansUiState(),
                paymentMethodsState = PaymentMethodsUiState(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
