package com.wasylews.mykompot.presentation.login

import androidx.compose.foundation.text.input.TextFieldState

data class LoginUiState(
    val usernameState: TextFieldState,
    val passwordState: TextFieldState,
    val isLoading: Boolean = false,
    val errorId: Int? = null
)
