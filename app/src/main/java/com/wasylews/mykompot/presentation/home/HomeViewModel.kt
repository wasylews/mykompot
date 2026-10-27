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

import androidx.lifecycle.ViewModel
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.GetStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
    private val getStatusUseCase: GetStatusUseCase,
): ViewModel() {

    private val _uiState =
        MutableStateFlow(
            HomeUiState(
                status = R.string.all_good,
                reason = R.string.everything_is_working,
                note = R.string.account_is_active,
                nextChargeDate = "1 Nov",
                countdown = "in 20 days",
                tariffName = "Standart 100",
                tariffPrice = "300 ₴",
            ),
        )
    val uiState: StateFlow<HomeUiState> = _uiState
}
