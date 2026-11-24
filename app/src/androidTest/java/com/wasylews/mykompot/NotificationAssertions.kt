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

package com.wasylews.mykompot

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.util.concurrent.TimeUnit

internal fun BaseAcceptanceTest.assertNoNotificationPosted() {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertEquals(0, notificationManager.activeNotifications.size)
}

internal fun BaseAcceptanceTest.grantNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }
}

internal fun BaseAcceptanceTest.cancelAllNotifications() {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancelAll()
}

internal fun BaseAcceptanceTest.assertNotificationPosted(
    titleRes: Int,
    textRes: Int,
) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val expectedTitle = stringRes(titleRes)
    val expectedText = stringRes(textRes)

    val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5)
    var posted: Notification? = null
    while (System.currentTimeMillis() < deadline) {
        posted = notificationManager.activeNotifications
            .map { it.notification }
            .firstOrNull { notification ->
                val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                title == expectedTitle && text == expectedText
            }
        if (posted != null) break
        Thread.sleep(100)
    }

    assertNotNull("Expected a notification to be posted", posted)
    val smallIcon = posted?.getSmallIcon()
    assertNotNull("Expected notification to have a small icon", smallIcon)
    assertEquals(R.drawable.ic_notification, smallIcon?.resId)
    assertNotNull("Expected notification to open the app", posted?.contentIntent)
}
