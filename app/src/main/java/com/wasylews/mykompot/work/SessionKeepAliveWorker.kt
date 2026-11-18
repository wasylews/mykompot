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

package com.wasylews.mykompot.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wasylews.mykompot.R
import com.wasylews.mykompot.domain.NotificationsPermission
import com.wasylews.mykompot.domain.RefreshStatus
import com.wasylews.mykompot.domain.model.RefreshResult
import com.wasylews.mykompot.presentation.main.MainActivity
import org.koin.android.annotation.KoinWorker

@KoinWorker
class SessionKeepAliveWorker(
    appContext: Context,
    params: WorkerParameters,
    private val refreshStatus: RefreshStatus,
    private val notificationManager: NotificationManager,
    private val notificationsPermission: NotificationsPermission,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = when (refreshStatus()) {
        RefreshResult.Expired -> {
            notifySessionExpired()
            Result.success()
        }

        is RefreshResult.Updated -> {
            Result.success()
        }

        is RefreshResult.Failed -> {
            Result.retry()
        }
    }

    private fun notifySessionExpired() {
        if (!notificationsPermission.isGranted()) return

        val channel = NotificationChannel(
            SESSION_CHANNEL_ID,
            applicationContext.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
        if (!notificationManager.canPostTo(SESSION_CHANNEL_ID)) return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, SESSION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.session_expired_notification_title))
            .setContentText(applicationContext.getString(R.string.session_expired_notification_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SESSION_EXPIRED_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val SESSION_CHANNEL_ID = "session"
        const val SESSION_EXPIRED_NOTIFICATION_ID = 1001
    }
}
