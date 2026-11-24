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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wasylews.mykompot.R
import com.wasylews.mykompot.ui.theme.BACKGROUND_DARK_ARGB
import com.wasylews.mykompot.ui.theme.BACKGROUND_LIGHT_ARGB
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import com.wasylews.mykompot.ui.theme.Spacing

const val REMEMBER_ME_TEST_TAG = "remember_me"

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onLoginClick: () -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onErrorConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val error = uiState.errorId?.let { stringResource(it) }.orEmpty()

    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            snackbarHostState.showSnackbar(error)
            onErrorConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LoginForm(
            uiState = uiState,
            onLoginClick = onLoginClick,
            onRememberMeChange = onRememberMeChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.lg),
        )
    }
}

@Composable
private fun LoginForm(
    uiState: LoginUiState,
    onLoginClick: () -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppLogo()
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.welcome_back),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        UsernameField(state = uiState.usernameState)
        Spacer(modifier = Modifier.height(Spacing.md))
        PasswordField(
            state = uiState.passwordState,
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible },
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        RememberMeCheckbox(
            checked = uiState.rememberMe,
            onCheckedChange = onRememberMeChange,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        LoginButton(isLoading = uiState.isLoading, onClick = onLoginClick)
    }
}

@Composable
private fun AppLogo(modifier: Modifier = Modifier) {
    // The launcher mipmap resolves to an adaptive icon XML on API 26+, which Compose cannot paint.
    // Rebuild the same icon from its vector foreground and the launcher background color.
    Box(
        modifier = modifier
            .size(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colorResource(R.color.launcher_background)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun RememberMeCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .semantics { testTag = REMEMBER_ME_TEST_TAG },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(stringResource(R.string.remember_me))
    }
}

@Composable
private fun UsernameField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = state,
        label = { Text(stringResource(R.string.username)) },
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.Username },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
private fun PasswordField(
    state: TextFieldState,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedSecureTextField(
        state = state,
        label = { Text(stringResource(R.string.password)) },
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.Password },
        textObfuscationMode = if (passwordVisible) {
            TextObfuscationMode.Visible
        } else {
            TextObfuscationMode.Hidden
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = stringResource(
                        if (passwordVisible) R.string.hide_password else R.string.show_password,
                    ),
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
    )
}

@Composable
private fun LoginButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonLabel = stringResource(if (isLoading) R.string.logging_in else R.string.log_in)
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = buttonLabel },
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(stringResource(R.string.log_in))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = BACKGROUND_LIGHT_ARGB,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO,
)
@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = BACKGROUND_DARK_ARGB,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun LoginScreenPreview() {
    MyKompotTheme {
        LoginScreen(
            uiState = LoginUiState(
                usernameState = TextFieldState("user"),
                passwordState = TextFieldState("pass"),
                isLoading = false,
            ),
            onLoginClick = {},
            onRememberMeChange = {},
            onErrorConsumed = {},
        )
    }
}
