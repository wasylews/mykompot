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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wasylews.mykompot.presentation.home.ktx.groupedCardBorder
import com.wasylews.mykompot.ui.components.shimmer
import com.wasylews.mykompot.ui.theme.Spacing

@Composable
internal fun PaymentsSkeletonList(
    rows: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(rows) { index ->
            PaymentRowSkeleton(
                isFirst = index == 0,
                isLast = index == rows - 1,
            )
        }
    }
}

@Composable
private fun PaymentRowSkeleton(
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
        PaymentRowSkeletonContent()
        if (!isLast) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun PaymentRowSkeletonContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(modifier = Modifier.size(40.dp), corner = 12.dp)
        Spacer(modifier = Modifier.width(Spacing.md))
        PaymentRowSkeletonDetails(modifier = Modifier.weight(1f))
        PaymentRowSkeletonAmount()
    }
}

@Composable
private fun PaymentRowSkeletonDetails(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp))
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f).height(12.dp))
    }
}

@Composable
private fun PaymentRowSkeletonAmount() {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SkeletonBlock(modifier = Modifier.width(56.dp).height(14.dp))
        SkeletonBlock(modifier = Modifier.width(64.dp).height(12.dp))
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    corner: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .shimmer(),
    )
}
