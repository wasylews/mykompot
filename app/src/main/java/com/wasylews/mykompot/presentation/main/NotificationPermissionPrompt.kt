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

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.NotificationsPermission

/**
 * Explains why the app wants to notify before asking for the runtime permission.
 *
 * Notifications are only an enhancement: when the permission is missing or denied, the workers
 * simply skip posting and the rest of the app keeps working.
 */
@Composable
internal fun NotificationPermissionPrompt(
    isLoggedIn: Boolean,
    notificationsPermission: NotificationsPermission,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    var showRationale by rememberSaveable { mutableStateOf(false) }
    var promptAnswered by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Nothing to do: notifications are optional. */ }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && !promptAnswered && !notificationsPermission.isGranted()) {
            showRationale = true
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                promptAnswered = true
                showRationale = false
            },
            title = { Text(stringResource(R.string.notifications_rationale_title)) },
            text = { Text(stringResource(R.string.notifications_rationale_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        promptAnswered = true
                        showRationale = false
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                ) {
                    Text(stringResource(R.string.notifications_allow))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        promptAnswered = true
                        showRationale = false
                    },
                ) {
                    Text(stringResource(R.string.notifications_not_now))
                }
            },
        )
    }
}
