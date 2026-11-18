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
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wasylews.mykompot.R
import com.wasylews.mykompot.data.LocalStorage
import com.wasylews.mykompot.domain.AppForegroundState
import com.wasylews.mykompot.domain.NotificationsPermission
import com.wasylews.mykompot.domain.model.PaymentDueThreshold
import com.wasylews.mykompot.presentation.main.MainActivity
import kotlinx.coroutines.flow.first
import org.koin.android.annotation.KoinWorker
import java.time.LocalDate

@KoinWorker
class PaymentDueReminderWorker(
    appContext: Context,
    params: WorkerParameters,
    private val localStorage: LocalStorage,
    private val appForegroundState: AppForegroundState,
    private val notificationManager: NotificationManager,
    private val notificationsPermission: NotificationsPermission,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val reminder = reminderInput()
        if (reminder != null && shouldNotify(reminder)) {
            notifyPaymentDue(reminder.threshold)
        }
        return Result.success()
    }

    private fun reminderInput(): ReminderInput? {
        val thresholdName = inputData.getString(PaymentDueReminderSchedulerImpl.KEY_THRESHOLD)
        val dueDateString = inputData.getString(PaymentDueReminderSchedulerImpl.KEY_DUE_DATE)
        if (thresholdName == null || dueDateString == null) return null

        val threshold = runCatching { PaymentDueThreshold.valueOf(thresholdName) }.getOrNull()
        val dueDate = runCatching { LocalDate.parse(dueDateString) }.getOrNull()
        return if (threshold != null && dueDate != null) ReminderInput(threshold, dueDate) else null
    }

    private suspend fun shouldNotify(reminder: ReminderInput): Boolean {
        if (appForegroundState.isInForeground()) return false
        val profile = localStorage.profileFlow().first()
        return profile != null && profile.isActive && profile.nextPaymentDate == reminder.dueDate
    }

    private data class ReminderInput(
        val threshold: PaymentDueThreshold,
        val dueDate: LocalDate,
    )

    private fun notifyPaymentDue(threshold: PaymentDueThreshold) {
        if (!notificationsPermission.isGranted()) return

        val channel = NotificationChannel(
            PAYMENT_CHANNEL_ID,
            applicationContext.getString(R.string.payment_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
        if (!notificationManager.canPostTo(PAYMENT_CHANNEL_ID)) return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val content = threshold.notificationContent()

        val notification = NotificationCompat.Builder(applicationContext, PAYMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(content.titleRes))
            .setContentText(applicationContext.getString(content.textRes))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(content.notificationId, notification)
    }

    private fun PaymentDueThreshold.notificationContent(): NotificationContent = when (this) {
        PaymentDueThreshold.THREE_DAYS_BEFORE -> NotificationContent(
            titleRes = R.string.payment_due_in_three_days_title,
            textRes = R.string.payment_due_in_three_days_text,
            notificationId = NOTIFICATION_ID_THREE_DAYS,
        )

        PaymentDueThreshold.ONE_DAY_BEFORE -> NotificationContent(
            titleRes = R.string.payment_due_tomorrow_title,
            textRes = R.string.payment_due_tomorrow_text,
            notificationId = NOTIFICATION_ID_ONE_DAY,
        )

        PaymentDueThreshold.DUE_DAY -> NotificationContent(
            titleRes = R.string.payment_due_today_title,
            textRes = R.string.payment_due_today_text,
            notificationId = NOTIFICATION_ID_DUE_DAY,
        )

        PaymentDueThreshold.OVERDUE -> NotificationContent(
            titleRes = R.string.payment_overdue_title,
            textRes = R.string.payment_overdue_text,
            notificationId = NOTIFICATION_ID_OVERDUE,
        )
    }

    private data class NotificationContent(
        @StringRes val titleRes: Int,
        @StringRes val textRes: Int,
        val notificationId: Int,
    )

    private companion object {
        const val PAYMENT_CHANNEL_ID = "payment_reminders"
        const val NOTIFICATION_ID_THREE_DAYS = 2001
        const val NOTIFICATION_ID_ONE_DAY = 2002
        const val NOTIFICATION_ID_DUE_DAY = 2003
        const val NOTIFICATION_ID_OVERDUE = 2004
    }
}
