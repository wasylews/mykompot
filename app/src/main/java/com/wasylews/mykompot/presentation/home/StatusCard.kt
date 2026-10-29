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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasylews.mykompot.R
import com.wasylews.mykompot.ui.theme.LocalStatusColors
import com.wasylews.mykompot.ui.theme.Spacing
import java.time.LocalDate

@Composable
fun StatusCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val statusColors = LocalStatusColors.current

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
                text = stringResource(R.string.next_charge).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.95.sp,
                color = onColor.copy(alpha = 0.65f),
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                state.nextChargeDate?.let { date ->
                    Text(
                        text = chargeDateLabel(date),
                        style = MaterialTheme.typography.titleMedium,
                        color = onColor,
                    )
                    state.daysUntilPayment?.let { days ->
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
        // Tariff link
        TariffBlock(onColor, state)
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
            text = state.tariffPrice,
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

@androidx.compose.ui.tooling.preview.Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = 0xFFF4F3FB,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO,
)
@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF0E0D13,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun StatusCardPreview() {
    com.wasylews.mykompot.ui.theme.MyKompotTheme {
        androidx.compose.material3.Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) { innerPadding ->
            StatusCard(
                state = HomeUiState(
                    status = R.string.status_all_good,
                    message = R.string.status_all_good_message,
                    nextChargeDate = LocalDate.of(2026, 11, 1),
                    daysUntilPayment = 20,
                    tariffName = "Standart 100",
                    tariffPrice = "300 ₴",
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding),
            )
        }
    }
}
