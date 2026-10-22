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
package com.wasylews.mykompot.presentation.status

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wasylews.mykompot.R
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.ui.theme.Spacing

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.your_internet),
            style = MaterialTheme.typography.titleLarge,
        )
        StatusCard(
            state =
                StatusCardState(
                    status = stringResource(R.string.all_good),
                    reason = stringResource(R.string.everything_is_working),
                    note = stringResource(R.string.account_is_active),
                    nextChargeDate = "1 Nov",
                    countdown = "in 20 days",
                    tariffName = "Standart 100",
                    tariffPrice = "300 ₴",
                ),
        )
    }
}

@Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = 0xFFF4F3FB,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF0E0D13,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun HomeScreenPreview() {
    MyKompotTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) { innerPadding ->
            HomeScreen(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            )
        }
    }
}
