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
import com.wasylews.mykompot.domain.PaymentDueNotifiedStore
import com.wasylews.mykompot.domain.PaymentDueReminderResolver
import com.wasylews.mykompot.domain.model.PaymentDueThreshold
import com.wasylews.mykompot.presentation.main.MainActivity
import kotlinx.coroutines.flow.first
import org.koin.android.annotation.KoinWorker
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/** Daily background check that posts at most one notice per billing period and threshold. */
@KoinWorker
class PaymentDueReminderWorker(
    appContext: Context,
    params: WorkerParameters,
    private val localStorage: LocalStorage,
    private val notifiedStore: PaymentDueNotifiedStore,
    private val appForegroundState: AppForegroundState,
    private val notificationManager: NotificationManager,
    private val notificationsPermission: NotificationsPermission,
    private val clock: Clock,
    private val zoneId: ZoneId,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val profile = localStorage.profileFlow().first()
        val today = clock.now().toJavaInstant().atZone(zoneId).toLocalDate()
        val reminder = PaymentDueReminderResolver.resolve(profile, today)

        val postedKey = reminder?.let { candidate ->
            val canPost = !appForegroundState.isInForeground() &&
                !notifiedStore.isNotified(candidate.ledgerKey) &&
                notificationsPermission.isGranted()
            if (canPost && postNotification(candidate.threshold)) candidate.ledgerKey else null
        }

        // Only a delivered notice consumes the ledger entry; permission/channel denials do not.
        if (postedKey != null) notifiedStore.markNotified(postedKey)
        return Result.success()
    }

    private fun postNotification(threshold: PaymentDueThreshold): Boolean {
        val content = notificationContent(threshold) ?: return false

        val channel = NotificationChannel(
            PAYMENT_CHANNEL_ID,
            applicationContext.getString(R.string.payment_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
        if (!notificationManager.canPostTo(PAYMENT_CHANNEL_ID)) return false

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, PAYMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(content.titleRes))
            .setContentText(applicationContext.getString(content.textRes))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(content.notificationId, notification)
        return true
    }

    private fun notificationContent(threshold: PaymentDueThreshold): NotificationContent? = when (threshold) {
        PaymentDueThreshold.THREE_DAYS_BEFORE -> NotificationContent(
            titleRes = R.string.payment_due_in_three_days_title,
            textRes = R.string.payment_due_in_three_days_text,
            notificationId = NOTIFICATION_ID_THREE_DAYS,
        )

        PaymentDueThreshold.DUE_DAY -> NotificationContent(
            titleRes = R.string.payment_due_today_title,
            textRes = R.string.payment_due_today_text,
            notificationId = NOTIFICATION_ID_DUE_DAY,
        )

        else -> null
    }

    private data class NotificationContent(
        @StringRes val titleRes: Int,
        @StringRes val textRes: Int,
        val notificationId: Int,
    )

    private companion object {
        const val PAYMENT_CHANNEL_ID = "payment_reminders"
        const val NOTIFICATION_ID_THREE_DAYS = 2001
        const val NOTIFICATION_ID_DUE_DAY = 2003
    }
}
