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

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.ui.theme.LocalStatusPalettes
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.ui.theme.Spacing
import com.wasylews.mykompot.ui.theme.StatusColors
import com.wasylews.mykompot.ui.theme.StatusPalettes

internal const val STATUS_TARIFF_NAME_TEST_TAG = "status_tariff_name"
internal const val STATUS_TARIFF_PRICE_TEST_TAG = "status_tariff_price"

@Composable
fun StatusCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onPayClick: () -> Unit = {},
    onTariffClick: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    when (val content = state.content) {
        is HomeContent.Content -> ContentCard(
            state = content,
            modifier = modifier,
            onPayClick = onPayClick,
            onTariffClick = onTariffClick,
        )

        HomeContent.Unknown -> UnknownCard(
            modifier = modifier,
            onRetry = onRetry,
        )

        HomeContent.Loading -> Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg)
                .testTag(HOME_LOADING_TEST_TAG),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ContentCard(
    state: HomeContent.Content,
    modifier: Modifier,
    onPayClick: () -> Unit,
    onTariffClick: () -> Unit,
) {
    val palettes = LocalStatusPalettes.current
    val statusColors = statusColorsFor(state.account.state, palettes)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = statusColors.container),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            StatusChip(status = state.status, onColor = statusColors.on)
            StatusWordAndMessage(
                status = state.status,
                message = state.message,
                onColor = statusColors.on,
            )
            NextChargeBlock(
                state = state,
                onColor = statusColors.on,
                onTariffClick = onTariffClick,
            )
            if (state.account.state == AccountStatus.State.PAYMENT_DUE ||
                state.account.state == AccountStatus.State.SUSPENDED
            ) {
                Button(
                    onClick = onPayClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColors.on,
                        contentColor = statusColors.container,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.pay_now),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnknownCard(
    modifier: Modifier,
    onRetry: () -> Unit,
) {
    val palettes = LocalStatusPalettes.current
    val onColor = palettes.unknown.on

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = palettes.unknown.container),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            StatusChip(status = R.string.status_unknown, onColor = onColor)
            StatusWordAndMessage(
                status = R.string.status_unknown,
                message = R.string.status_unknown_message,
                onColor = onColor,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onColor,
                    contentColor = palettes.unknown.container,
                ),
            ) {
                Text(
                    text = stringResource(R.string.retry),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun statusColorsFor(
    state: AccountStatus.State,
    palettes: StatusPalettes,
): StatusColors = when (state) {
    AccountStatus.State.ALL_GOOD -> palettes.allGood
    AccountStatus.State.PAYMENT_DUE -> palettes.paymentDue
    AccountStatus.State.SUSPENDED -> palettes.suspended
}

@Composable
private fun StatusChip(
    @StringRes status: Int,
    onColor: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.12f))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(onColor),
        )
        Text(
            text = stringResource(status).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.66.sp,
            color = onColor,
        )
    }
}

@Composable
private fun StatusWordAndMessage(
    @StringRes status: Int,
    @StringRes message: Int,
    onColor: Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(status),
            style = MaterialTheme.typography.displaySmall,
            color = onColor,
        )
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = onColor.copy(alpha = 0.88f),
        )
    }
}

@Composable
private fun NextChargeBlock(
    state: HomeContent.Content,
    onColor: Color,
    onTariffClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.nextChargeLabel?.let {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(state.nextChargeLabel).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.95.sp,
                    color = onColor.copy(alpha = 0.65f),
                )
                ChargeValue(state = state, onColor = onColor)
            }
        }
        // Tariff link
        TariffBlock(onColor = onColor, state = state, onClick = onTariffClick)
    }
}

@Composable
private fun ChargeValue(
    state: HomeContent.Content,
    onColor: Color,
) {
    val isPaymentDue = state.account.state == AccountStatus.State.PAYMENT_DUE

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        val date = state.account.profile.nextPaymentDate ?: return
        Text(
            text = chargeDateLabel(date),
            style = MaterialTheme.typography.titleMedium,
            color = onColor,
        )
        val days = state.account.daysLeft ?: return
        if (!isPaymentDue) {
            Text(
                text = relativeDayLabel(days),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = onColor.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun TariffBlock(
    onColor: Color,
    state: HomeContent.Content,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.tariff),
            style = MaterialTheme.typography.bodySmall,
            color = onColor.copy(alpha = 0.68f),
        )
        Text(
            text = state.account.profile.tariffPlan.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = onColor,
            modifier = Modifier.testTag(STATUS_TARIFF_NAME_TEST_TAG),
        )
        Text(
            text = stringResource(R.string.tariff_price, state.account.profile.tariffPlan.price),
            style = MaterialTheme.typography.bodySmall,
            color = onColor.copy(alpha = 0.68f),
            modifier = Modifier.testTag(STATUS_TARIFF_PRICE_TEST_TAG),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = onColor.copy(alpha = 0.6f),
        )
    }
}

@Preview(name = "States (Light | Dark)", showBackground = true, widthDp = 800)
@Composable
fun StatusCardStatesPreview(
    @PreviewParameter(HomeUiStatePreviewParameterProvider::class) state: HomeUiState,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(false to Color(0xFFF4F3FB), true to Color(0xFF0E0D13)).forEach { (dark, bg) ->
            MyKompotTheme(darkTheme = dark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = bg,
                ) {
                    StatusCard(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                    )
                }
            }
        }
    }
}
