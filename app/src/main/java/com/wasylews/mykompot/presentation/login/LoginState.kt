package com.wasylews.mykompot.presentation.login

sealed interface LoginState {
    data object Unknown : LoginState
    data object Loading : LoginState
    data object LoggedIn : LoginState
    data class Error(val errorId: Int) : LoginState
}
