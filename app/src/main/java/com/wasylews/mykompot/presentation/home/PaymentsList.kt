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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wasylews.mykompot.R
import com.wasylews.mykompot.presentation.home.ktx.groupedCardBorder
import com.wasylews.mykompot.presentation.payments.paymentMethods
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.ui.theme.Spacing

internal const val PAYMENTS_CONTENT_TYPE = "payment"

private const val PAYMENTS_TITLE_KEY = "payments_title"
private const val PAYMENTS_STATE_KEY = "payments_state"
private const val LOADING_MORE_KEY = "payments_loading_more"
private const val LOAD_MORE_ERROR_KEY = "payments_load_more_error"
private const val LOAD_MORE_KEY = "payments_load_more"
private const val EMPTY_STATE_HEIGHT_FRACTION = 0.5f
private val PAYMENT_CARD_CORNER = 20.dp
private val PAYMENT_CARD_BORDER = 1.dp

internal fun LazyListScope.paymentsSection(
    paymentsState: PaymentsUiState,
    onLoadMorePayments: () -> Unit,
    onRetryLoadMorePayments: () -> Unit,
    onRetry: () -> Unit,
) {
    item(key = PAYMENTS_TITLE_KEY) {
        PaymentsTitle()
    }

    if (paymentsState.items.isEmpty()) {
        paymentsPlaceholder(paymentsState = paymentsState, onRetry = onRetry)
    } else {
        paymentItems(
            paymentsState = paymentsState,
            onLoadMorePayments = onLoadMorePayments,
            onRetryLoadMorePayments = onRetryLoadMorePayments,
        )
    }
}

@Composable
private fun PaymentsTitle() {
    Text(
        text = stringResource(R.string.payments),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = Spacing.md),
    )
}

private fun LazyListScope.paymentsPlaceholder(
    paymentsState: PaymentsUiState,
    onRetry: () -> Unit,
) {
    val errorId = paymentsState.errorId
    item(key = PAYMENTS_STATE_KEY) {
        PaymentsCenteredState(Modifier.fillParentMaxHeight(EMPTY_STATE_HEIGHT_FRACTION)) {
            when {
                paymentsState.isLoading -> CircularProgressIndicator()

                errorId != null -> PaymentsErrorContent(errorId = errorId, onRetry = onRetry)

                else -> Text(
                    text = stringResource(R.string.no_payments_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun LazyListScope.paymentItems(
    paymentsState: PaymentsUiState,
    onLoadMorePayments: () -> Unit,
    onRetryLoadMorePayments: () -> Unit,
) {
    val items = paymentsState.items
    itemsIndexed(
        items = items,
        key = { index, _ -> "payment_$index" },
        contentType = { _, _ -> PAYMENTS_CONTENT_TYPE },
    ) { index, item ->
        PaymentRowItem(
            item = item,
            isFirst = index == 0,
            isLast = index == items.lastIndex,
        )
    }

    if (paymentsState.isLoadingMore) {
        item(key = LOADING_MORE_KEY) {
            PaymentsCenteredState(modifier = Modifier.padding(vertical = Spacing.md)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }

    val loadMoreErrorId = paymentsState.loadMoreErrorId
    if (loadMoreErrorId != null) {
        item(key = LOAD_MORE_ERROR_KEY) {
            LoadMoreError(
                errorId = loadMoreErrorId,
                onRetry = onRetryLoadMorePayments,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
    }

    if (paymentsState.hasMore && !paymentsState.isLoadingMore && loadMoreErrorId == null) {
        item(key = LOAD_MORE_KEY) {
            LaunchedEffect(items.size) {
                onLoadMorePayments()
            }
        }
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
private fun PaymentRowItem(
    item: PaymentItemUi,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(groupedRowShape(isFirst = isFirst, isLast = isLast, corner = PAYMENT_CARD_CORNER))
            .background(MaterialTheme.colorScheme.surface)
            .groupedCardBorder(
                isFirst = isFirst,
                isLast = isLast,
                corner = PAYMENT_CARD_CORNER,
                stroke = PAYMENT_CARD_BORDER,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        PaymentRow(item = item)
        if (!isLast) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * Shape of a single row inside a card shared by adjacent lazy items: rounded outer corners on the
 * first and the last row, square edges in between.
 */
private fun groupedRowShape(
    isFirst: Boolean,
    isLast: Boolean,
    corner: Dp,
): Shape = when {
    isFirst && isLast -> RoundedCornerShape(corner)

    isFirst -> RoundedCornerShape(
        topStart = corner,
        topEnd = corner,
    )

    isLast -> RoundedCornerShape(
        bottomStart = corner,
        bottomEnd = corner,
    )

    else -> RectangleShape
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
        "Empty" to PaymentsUiState(),
        "Loading" to PaymentsUiState(isLoading = true),
        "Error" to PaymentsUiState(errorId = R.string.error_no_connection),
        "Loaded" to loaded,
        "Single payment" to loaded.copy(items = loaded.items.take(1)),
        "Has more" to loaded.copy(hasMore = true),
        "Loading more" to loaded.copy(items = loaded.items.take(2), hasMore = true, isLoadingMore = true),
        "Load more error" to loaded.copy(
            items = loaded.items.take(1),
            hasMore = true,
            loadMoreErrorId = R.string.error_no_connection,
        ),
    )

    override val values: Sequence<PaymentsUiState> = states.map { (_, state) -> state }.asSequence()

    override fun getDisplayName(index: Int): String = states[index].first
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.md),
                    ) {
                        paymentsSection(
                            paymentsState = state,
                            onLoadMorePayments = {},
                            onRetryLoadMorePayments = {},
                            onRetry = {},
                        )
                    }
                }
            }
        }
    }
}
