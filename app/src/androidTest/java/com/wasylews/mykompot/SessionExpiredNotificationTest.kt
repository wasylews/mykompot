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
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.wasylews.mykompot.work.SessionKeepAliveWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.androidx.workmanager.factory.KoinWorkerFactory

class SessionExpiredNotificationTest : BaseAcceptanceTest() {
    @Test
    fun clearsSessionAndPostsNotificationWhenBackgroundRefreshExpires() {
        // GIVEN I am logged in and the server treats the session as expired
        givenStoredSession(profile = allGoodProfile())
        givenHomeResponses(profileBody = HtmlFixtures.loginFailed())
        grantNotificationPermission()

        // WHEN the keep-alive background refresh runs
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = runBlocking {
            TestListenableWorkerBuilder<SessionKeepAliveWorker>(context)
                .setWorkerFactory(KoinWorkerFactory())
                .build()
                .doWork()
        }

        // THEN my session is cleared and I am notified to log in again
        assertEquals(ListenableWorker.Result.success(), result)
        assertSessionCleared()
        assertNotificationPosted(
            titleRes = R.string.session_expired_notification_title,
            textRes = R.string.session_expired_notification_text,
        )
    }

    private fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                InstrumentationRegistry.getInstrumentation().targetContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }
}
