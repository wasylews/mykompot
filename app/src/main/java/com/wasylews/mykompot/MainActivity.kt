package com.wasylews.mykompot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wasylews.mykompot.presentation.login.LoginScreen
import com.wasylews.mykompot.presentation.login.LoginState
import com.wasylews.mykompot.presentation.login.LoginViewModel
import com.wasylews.mykompot.presentation.status.HomeScreen
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyKompotTheme {
                val loginViewModel = koinViewModel<LoginViewModel>()
                val loginState by loginViewModel.loginState.collectAsState()
                val uiState by loginViewModel.uiState.collectAsState()
                val isLoggedIn = loginState == LoginState.LoggedIn

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) { innerPadding ->
                    if (isLoggedIn) {
                        HomeScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        LoginScreen(
                            uiState = uiState,
                            onLoginClick = { loginViewModel.login() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
