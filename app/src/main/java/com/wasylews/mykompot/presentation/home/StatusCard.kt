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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.model.AccountStatus
import com.wasylews.mykompot.ui.theme.LocalStatusPalettes
import com.wasylews.mykompot.ui.theme.Spacing

@Composable
fun StatusCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onPayClick: () -> Unit = {},
) {
    val palettes = LocalStatusPalettes.current
    val statusColors = when (state.accountState) {
        AccountStatus.State.ALL_GOOD -> palettes.allGood
        AccountStatus.State.PAYMENT_DUE -> palettes.paymentDue
        AccountStatus.State.UNKNOWN -> palettes.unknown
    }

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
            StatusWordAndMessage(state = state, onColor = statusColors.on)
            NextChargeBlock(state = state, onColor = statusColors.on)
            if (state.accountState == AccountStatus.State.PAYMENT_DUE) {
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
    state: HomeUiState,
    onColor: Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(state.status),
            style = MaterialTheme.typography.displaySmall,
            color = onColor,
        )
        Text(
            text = stringResource(state.message),
            style = MaterialTheme.typography.bodyMedium,
            color = onColor.copy(alpha = 0.88f),
        )
    }
}

@Composable
private fun NextChargeBlock(
    state: HomeUiState,
    onColor: Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
        // Tariff link
        TariffBlock(onColor, state)
    }
}

@Composable
private fun ChargeValue(
    state: HomeUiState,
    onColor: Color,
) {
    val isPaymentDue = state.accountState == AccountStatus.State.PAYMENT_DUE

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        val date = state.nextChargeDate
        if (date != null) {
            Text(
                text = chargeDateLabel(date),
                style = MaterialTheme.typography.titleMedium,
                color = onColor,
            )
            val days = state.daysUntilPayment
            if (!isPaymentDue && days != null) {
                Text(
                    text = relativeDayLabel(days),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onColor.copy(alpha = 0.62f),
                )
            }
        }
    }
}

@Composable
private fun TariffBlock(
    onColor: Color,
    state: HomeUiState,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.tariff),
            style = MaterialTheme.typography.bodySmall,
            color = onColor.copy(alpha = 0.68f),
        )
        Text(
            text = state.tariffName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = onColor,
        )
        Text(
            text = stringResource(R.string.tariff_price, state.tariffPrice),
            style = MaterialTheme.typography.bodySmall,
            color = onColor.copy(alpha = 0.68f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = onColor.copy(alpha = 0.6f),
        )
    }
}

@PreviewLightDark
@Composable
fun StatusCardPreview(
    @PreviewParameter(HomeUiStatePreviewParameterProvider::class) state: HomeUiState,
) {
    com.wasylews.mykompot.ui.theme.MyKompotTheme {
        androidx.compose.material3.Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) { innerPadding ->
            StatusCard(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding),
            )
        }
    }
}
