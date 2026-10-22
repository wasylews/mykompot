package com.wasylews.mykompot

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wasylews.mykompot.ui.theme.Spacing
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.wasylews.mykompot.ui.theme.MyKompotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyKompotTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome_back),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.log_in))
        }
    }
}

@Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = 0xFFF4F3FB,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF0E0D13,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LoginScreenPreview() {
    MyKompotTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            LoginScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.your_internet),
            style = MaterialTheme.typography.titleLarge
        )
        StatusCard(
            state = StatusCardState(
                status = stringResource(R.string.all_good),
                reason = stringResource(R.string.everything_is_working),
                note = stringResource(R.string.account_is_active),
                nextChargeDate = "1 Nov",
                countdown = "in 20 days",
                tariffName = "Standart 100",
                tariffPrice = "300 ₴"
            )
        )
    }
}

@Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = 0xFFF4F3FB,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF0E0D13,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun HomeScreenPreview() {
    MyKompotTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            HomeScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
