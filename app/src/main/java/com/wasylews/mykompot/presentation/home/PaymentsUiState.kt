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
import com.wasylews.mykompot.presentation.payments.PaymentMethod

data class PaymentsUiState(
    val items: List<PaymentItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    @StringRes val errorId: Int? = null,
    @StringRes val loadMoreErrorId: Int? = null,
)

data class PaymentItemUi(
    val amount: Int,
    val tariffName: String,
    val paymentMethod: PaymentMethod?,
    val dateTimeLabel: String,
)
