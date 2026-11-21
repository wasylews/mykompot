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

package com.wasylews.mykompot.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.NotificationsPermission
import com.wasylews.mykompot.domain.model.SessionState
import com.wasylews.mykompot.presentation.home.HomeActions
import com.wasylews.mykompot.presentation.home.HomeScreen
import com.wasylews.mykompot.presentation.home.HomeViewModel
import com.wasylews.mykompot.presentation.home.PaymentsViewModel
import com.wasylews.mykompot.presentation.login.LoginScreen
import com.wasylews.mykompot.presentation.login.LoginViewModel
import com.wasylews.mykompot.presentation.payments.PaymentMethod
import com.wasylews.mykompot.presentation.payments.PaymentMethodsViewModel
import com.wasylews.mykompot.presentation.tariffs.TariffPlansViewModel
import com.wasylews.mykompot.ui.theme.MyKompotTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyKompotTheme {
                MainContent(onPaymentMethodClick = ::openPaymentMethod)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainContent(onPaymentMethodClick: (PaymentMethod) -> Unit) {
        val mainViewModel = koinViewModel<MainViewModel>()
        val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()
        val loginViewModel = koinViewModel<LoginViewModel>()
        val uiState by loginViewModel.uiState.collectAsState()
        val notificationsPermission = koinInject<NotificationsPermission>()

        NotificationPermissionPrompt(
            isLoggedIn = sessionState == SessionState.LoggedIn,
            notificationsPermission = notificationsPermission,
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                if (sessionState == SessionState.LoggedIn) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        actions = {
                            TextButton(onClick = { mainViewModel.logOut() }) {
                                Text(stringResource(R.string.log_out))
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            when (sessionState) {
                SessionState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                SessionState.LoggedIn -> LoggedInContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onPaymentMethodClick = onPaymentMethodClick,
                )

                SessionState.LoggedOut -> LoginScreen(
                    uiState = uiState,
                    onLoginClick = { loginViewModel.login() },
                    onRememberMeChange = loginViewModel::setRememberMe,
                    onErrorConsumed = loginViewModel::clearError,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }

    @Composable
    private fun LoggedInContent(
        modifier: Modifier,
        onPaymentMethodClick: (PaymentMethod) -> Unit,
    ) {
        val homeViewModel = koinViewModel<HomeViewModel>()
        val tariffPlansViewModel = koinViewModel<TariffPlansViewModel>()
        val paymentMethodsViewModel = koinViewModel<PaymentMethodsViewModel>()
        val paymentsViewModel = koinViewModel<PaymentsViewModel>()
        val homeUiState by homeViewModel.uiState.collectAsState()
        val tariffPlansState by tariffPlansViewModel.uiState.collectAsState()
        val paymentMethodsState by paymentMethodsViewModel.uiState.collectAsState()
        val paymentsState by paymentsViewModel.uiState.collectAsState()

        HomeScreen(
            state = homeUiState,
            tariffPlansState = tariffPlansState,
            paymentMethodsState = paymentMethodsState,
            paymentsState = paymentsState,
            modifier = modifier,
            actions = HomeActions(
                onPullToRefresh = {
                    homeViewModel.refresh(userInitiated = true)
                    paymentsViewModel.refresh()
                },
                onRetry = { homeViewModel.refresh() },
                onPayClick = paymentMethodsViewModel::open,
                onTariffClick = tariffPlansViewModel::open,
                onTariffRetry = tariffPlansViewModel::retry,
                onTariffSheetDismiss = tariffPlansViewModel::dismiss,
                onPaymentMethodsDismiss = paymentMethodsViewModel::dismiss,
                onPaymentMethodClick = { method ->
                    paymentMethodsViewModel.dismiss()
                    onPaymentMethodClick(method)
                },
                onLoadMorePayments = paymentsViewModel::loadMorePayments,
                onRetryLoadMorePayments = paymentsViewModel::retryLoadMorePayments,
                onRetryPayments = paymentsViewModel::refresh,
            ),
        )
    }

    private fun openPaymentMethod(method: PaymentMethod) {
        val launchIntent = method.packageName?.let(packageManager::getLaunchIntentForPackage)
        if (launchIntent != null) {
            runCatching { startActivity(launchIntent) }
            return
        }

        val fallbackUrl = getString(method.fallbackUrlRes)
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, fallbackUrl.toUri())) }
    }
}
