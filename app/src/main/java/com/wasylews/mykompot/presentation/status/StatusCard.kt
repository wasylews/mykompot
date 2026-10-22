package com.wasylews.mykompot.presentation.status

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasylews.mykompot.R
import com.wasylews.mykompot.ui.theme.LocalStatusColors
import com.wasylews.mykompot.ui.theme.Spacing

data class StatusCardState(
    val status: String,
    val reason: String,
    val note: String,
    val nextChargeDate: String,
    val countdown: String,
    val tariffName: String,
    val tariffPrice: String
)

@Composable
fun StatusCard(
    state: StatusCardState,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = statusColors.container),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.12f))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColors.on)
                )
                Text(
                    text = state.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.66.sp,
                    color = statusColors.on
                )
            }

            // Word + reason
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.displaySmall,
                    color = statusColors.on
                )
                Text(
                    text = buildString {
                        append(state.reason)
                        if (state.note.isNotEmpty()) append(" ${state.note}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColors.on.copy(alpha = 0.88f)
                )
            }

            // Next-charge block
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.next_charge).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.95.sp,
                        color = statusColors.on.copy(alpha = 0.65f)
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            text = state.nextChargeDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = statusColors.on
                        )
                        Text(
                            text = state.countdown,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColors.on.copy(alpha = 0.62f)
                        )
                    }
                }
                // Tariff link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tariff),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors.on.copy(alpha = 0.68f)
                    )
                    Text(
                        text = state.tariffName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColors.on
                    )
                    Text(
                        text = state.tariffPrice,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors.on.copy(alpha = 0.68f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = statusColors.on.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = 0xFFF4F3FB,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF0E0D13,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun StatusCardPreview() {
    MyKompotTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            StatusCard(
                state = StatusCardState(
                    status = "All Good",
                    reason = "Everything is working.",
                    note = "Your account is active.",
                    nextChargeDate = "1 Nov",
                    countdown = "in 20 days",
                    tariffName = "Standart 100",
                    tariffPrice = "300 ₴"
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
            )
        }
    }
}

