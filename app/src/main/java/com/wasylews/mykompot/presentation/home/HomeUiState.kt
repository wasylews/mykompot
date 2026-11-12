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
import com.wasylews.mykompot.domain.model.AccountStatus

data class HomeUiState(
    val content: HomeContent = HomeContent.Loading,
    val isRefreshing: Boolean = false,
    @StringRes val snackbarMessage: Int? = null,
)

sealed interface HomeContent {
    data object Loading : HomeContent

    data class Content(
        val account: AccountStatus.Known,
        @StringRes val status: Int,
        @StringRes val message: Int,
        @StringRes val nextChargeLabel: Int?,
    ) : HomeContent

    data object Unavailable : HomeContent
}
