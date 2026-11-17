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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.wasylews.mykompot.R
import com.wasylews.mykompot.presentation.payments.paymentMethods
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.ui.theme.Spacing

internal const val PAYMENTS_LIST_TEST_TAG = "payments_list"

@Composable
fun PaymentsList(
    paymentsState: PaymentsUiState,
    onLoadMorePayments: () -> Unit,
    onRetryLoadMorePayments: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    ObservePaymentsLoadMore(listState = listState, paymentsState = paymentsState, onLoadMorePayments = onLoadMorePayments)

    val showItemsList = paymentsState.items.isNotEmpty() ||
        !(paymentsState.isLoading || paymentsState.errorId != null)

    Column(
        modifier = if (showItemsList) modifier else modifier.testTag(PAYMENTS_LIST_TEST_TAG),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.payments),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        PaymentsListBody(
            paymentsState = paymentsState,
            listState = listState,
            onRetry = onRetry,
            onRetryLoadMorePayments = onRetryLoadMorePayments,
        )
    }
}

@Composable
private fun ObservePaymentsLoadMore(
    listState: androidx.compose.foundation.lazy.LazyListState,
    paymentsState: PaymentsUiState,
    onLoadMorePayments: () -> Unit,
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            paymentsState.hasMore &&
                !paymentsState.isLoadingMore &&
                paymentsState.loadMoreErrorId == null &&
                total > 0 &&
                lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMorePayments()
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.PaymentsListBody(
    paymentsState: PaymentsUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRetry: () -> Unit,
    onRetryLoadMorePayments: () -> Unit,
) {
    when {
        paymentsState.isLoading && paymentsState.items.isEmpty() -> PaymentsCenteredState(Modifier.weight(1f)) {
            CircularProgressIndicator()
        }

        paymentsState.errorId != null && paymentsState.items.isEmpty() -> PaymentsCenteredState(Modifier.weight(1f)) {
            PaymentsErrorContent(errorId = paymentsState.errorId, onRetry = onRetry)
        }

        paymentsState.items.isEmpty() -> PaymentsCenteredState(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.no_payments_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        else -> PaymentsItemsColumn(
            paymentsState = paymentsState,
            listState = listState,
            onRetryLoadMorePayments = onRetryLoadMorePayments,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PaymentsCenteredState(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PaymentsErrorContent(
    errorId: Int,
    onRetry: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(errorId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun PaymentsItemsColumn(
    paymentsState: PaymentsUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRetryLoadMorePayments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag(PAYMENTS_LIST_TEST_TAG),
        contentPadding = PaddingValues(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { PaymentsCard(items = paymentsState.items) }

        if (paymentsState.isLoadingMore) {
            item {
                PaymentsCenteredState(modifier = Modifier.padding(vertical = Spacing.md)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        paymentsState.loadMoreErrorId?.let { errorId ->
            item { LoadMoreError(errorId = errorId, onRetry = onRetryLoadMorePayments) }
        }
    }
}

@Composable
private fun PaymentsCard(
    items: List<PaymentItemUi>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, payment ->
                PaymentRow(item = payment)
                if (index < items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun LoadMoreError(
    errorId: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(errorId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
fun PaymentRow(
    item: PaymentItemUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaymentRowIcon(item = item)
        Spacer(modifier = Modifier.width(Spacing.md))
        PaymentRowDetails(item = item, modifier = Modifier.weight(1f))
        PaymentRowAmount(item = item)
    }
}

@Composable
private fun PaymentRowIcon(item: PaymentItemUi) {
    if (item.paymentMethod != null) {
        Image(
            painter = painterResource(item.paymentMethod.iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.currency_symbol),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaymentRowDetails(
    item: PaymentItemUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = item.tariffName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        item.paymentMethod?.let { method ->
            Text(
                text = stringResource(method.titleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaymentRowAmount(item: PaymentItemUi) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.tariff_price, item.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = item.dateTimeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun samplePaymentsState(): PaymentsUiState = PaymentsUiState(
    items = listOf(
        PaymentItemUi(
            amount = 300,
            tariffName = "Standart 100",
            paymentMethod = paymentMethods.first { it.titleRes == R.string.payment_method_privat24 },
            dateTimeLabel = "1 Oct, 10:05",
        ),
        PaymentItemUi(
            amount = 300,
            tariffName = "Standart 100",
            paymentMethod = paymentMethods.first { it.titleRes == R.string.payment_method_privat24 },
            dateTimeLabel = "1 Sep, 09:51",
        ),
        PaymentItemUi(
            amount = 250,
            tariffName = "Econom 100",
            paymentMethod = paymentMethods.first { it.titleRes == R.string.payment_method_easypay },
            dateTimeLabel = "15 Oct 2024, 09:39",
        ),
        PaymentItemUi(
            amount = 300,
            tariffName = "Standart 100",
            paymentMethod = null,
            dateTimeLabel = "6 Aug 2026, 00:00",
        ),
    ),
    hasMore = false,
)

class PaymentsUiStatePreviewParameterProvider : PreviewParameterProvider<PaymentsUiState> {
    private val loaded = samplePaymentsState()
    private val states = listOf(
        PaymentsUiState(),
        PaymentsUiState(isLoading = true),
        PaymentsUiState(errorId = R.string.error_no_connection),
        loaded,
        loaded.copy(hasMore = true),
        loaded.copy(items = loaded.items.take(2), hasMore = true, isLoadingMore = true),
        loaded.copy(items = loaded.items.take(1), hasMore = true, loadMoreErrorId = R.string.error_no_connection),
    )

    override val values: Sequence<PaymentsUiState> = states.asSequence()

    override fun getDisplayName(index: Int): String = when (index) {
        0 -> "Empty"
        1 -> "Loading"
        2 -> "Error"
        3 -> "Loaded"
        4 -> "Has more"
        5 -> "Loading more"
        6 -> "Load more error"
        else -> index.toString()
    }
}

@Preview(name = "States (Light | Dark)", showBackground = true, widthDp = 800)
@Composable
fun PaymentsListStatesPreview(
    @PreviewParameter(PaymentsUiStatePreviewParameterProvider::class) state: PaymentsUiState,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(false to Color(0xFFF4F3FB), true to Color(0xFF0E0D13)).forEach { (dark, bg) ->
            MyKompotTheme(darkTheme = dark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = bg,
                ) {
                    PaymentsList(
                        paymentsState = state,
                        onLoadMorePayments = {},
                        onRetryLoadMorePayments = {},
                        onRetry = {},
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.md),
                    )
                }
            }
        }
    }
}
