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
import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LoginViewModel(
    private val repository: PortalRepository,
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

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Unknown)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    val uiState: StateFlow<LoginUiState> =
        loginState
            .map { state ->
                LoginUiState(
                    usernameState = usernameState,
                    passwordState = passwordState,
                    isLoading = state is LoginState.Loading,
                    errorId = (state as? LoginState.Error)?.errorId,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, LoginUiState(usernameState, passwordState))

    fun login() {
        if (!isUsernameValid || !isPasswordValid) return

        _loginState.value = LoginState.Loading
        when (repository.login(usernameState.text.toString(), passwordState.text.toString())) {
            LoginResult.Success -> _loginState.value = LoginState.LoggedIn
            LoginResult.InvalidCredentials -> _loginState.value = LoginState.Error(R.string.error_invalid_credentials)
            LoginResult.NetworkError -> _loginState.value = LoginState.Error(R.string.error_no_connection)
            LoginResult.ServerError -> _loginState.value = LoginState.Error(R.string.error_server)
        }
    }
}
