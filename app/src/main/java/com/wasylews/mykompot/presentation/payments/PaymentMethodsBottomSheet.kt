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

package com.wasylews.mykompot.presentation.payments

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wasylews.mykompot.R
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.presentation.home.PreviewLightDark
import com.wasylews.mykompot.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsBottomSheet(
    methods: List<PaymentMethod>,
    onMethodClick: (PaymentMethod) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        PaymentMethodsContent(
            methods = methods,
            onMethodClick = onMethodClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
        )
    }
}

@Composable
fun PaymentMethodsContent(
    methods: List<PaymentMethod>,
    onMethodClick: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.payment_methods),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(top = Spacing.xs),
        ) {
            methods.forEach { method ->
                PaymentMethodRow(
                    method = method,
                    onClick = { onMethodClick(method) },
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    method: PaymentMethod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Image(
            painter = painterResource(method.iconRes),
            contentDescription = stringResource(method.titleRes),
            modifier = Modifier
                .size(40.dp)
                .semantics { paymentMethodIconRes = method.iconRes },
        )
        Text(
            text = stringResource(method.titleRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@PreviewLightDark
@Composable
private fun PaymentMethodsContentPreview() {
    MyKompotTheme {
        Surface {
            PaymentMethodsContent(
                methods = paymentMethods,
                onMethodClick = {},
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
