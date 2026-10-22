package com.wasylews.mykompot.presentation.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.model.LoginResult
import com.wasylews.mykompot.domain.repository.PortalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(
    private val repository: PortalRepository = object : PortalRepository {
        override fun login(username: String, password: String) = LoginResult.Success
    }
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
