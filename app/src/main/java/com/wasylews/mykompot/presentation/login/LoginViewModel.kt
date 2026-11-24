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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.PaymentDueReminderScheduler
import com.wasylews.mykompot.domain.model.Credentials
import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.repository.CredentialsStore
import com.wasylews.mykompot.domain.repository.PortalRepository
import com.wasylews.mykompot.presentation.toErrorId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LoginViewModel(
    private val repository: PortalRepository,
    private val paymentDueReminderScheduler: PaymentDueReminderScheduler,
    private val credentialsStore: CredentialsStore,
) : ViewModel() {
    private val inputValidationRegex = """\w+""".toRegex()

    val usernameState = TextFieldState()
    val passwordState = TextFieldState()

    val isUsernameValid by derivedStateOf {
        usernameState.text.matches(inputValidationRegex)
    }

    val isPasswordValid by derivedStateOf {
        passwordState.text.matches(inputValidationRegex)
    }

    private val _uiState = MutableStateFlow(LoginUiState(usernameState, passwordState))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login() = viewModelScope.launch {
        if (!isUsernameValid || !isPasswordValid) return@launch

        val credentials = Credentials(
            username = usernameState.text.toString(),
            password = passwordState.text.toString(),
        )
        val rememberMe = _uiState.value.rememberMe
        _uiState.update { it.copy(isLoading = true, errorId = null) }
        val errorId = when (val result = repository.login(credentials)) {
            is LoginResult.Success -> {
                if (rememberMe) {
                    credentialsStore.save(credentials)
                }
                paymentDueReminderScheduler.schedule(result.profile)
                null
            }

            LoginResult.InvalidCredentials -> {
                credentialsStore.clear()
                R.string.error_invalid_credentials
            }

            is LoginResult.Failed -> {
                result.reason.toErrorId(parseErrorId = R.string.error_parsing)
            }
        }
        _uiState.update { it.copy(isLoading = false, errorId = errorId) }
    }

    fun setRememberMe(rememberMe: Boolean) {
        _uiState.update { it.copy(rememberMe = rememberMe) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorId = null) }
    }
}
