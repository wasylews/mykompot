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
package com.wasylews.mykompot.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wasylews.mykompot.R
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.ui.theme.Spacing

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val loginViewModel = remember { LoginViewModel() }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.welcome_back),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        TextField(
            state = loginViewModel.usernameState,
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        SecureTextField(
            state = loginViewModel.passwordState,
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Button(
            onClick = { loginViewModel.login() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.log_in))
        }
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
fun LoginScreenPreview() {
    MyKompotTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) { innerPadding ->
            LoginScreen(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            )
        }
    }
}
