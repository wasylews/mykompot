package com.wasylews.mykompot.domain.model

sealed interface LoginResult {
    data object Success : LoginResult
    data object InvalidCredentials : LoginResult
    data object NetworkError : LoginResult
    data object ServerError : LoginResult
}
