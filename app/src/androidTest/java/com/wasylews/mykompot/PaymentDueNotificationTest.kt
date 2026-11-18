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

import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.wasylews.mykompot.domain.AppForegroundState
import com.wasylews.mykompot.domain.model.PaymentDueThreshold
import com.wasylews.mykompot.work.PaymentDueReminderSchedulerImpl
import com.wasylews.mykompot.work.PaymentDueReminderWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module

class PaymentDueNotificationTest : BaseAcceptanceTest() {
    private val backgroundModule = module {
        single<AppForegroundState> {
            object : AppForegroundState {
                override fun isInForeground(): Boolean = false
            }
        }
    }

    @Before
    fun setUpBackgroundState() {
        loadKoinModules(backgroundModule)
    }

    @After
    fun tearDownBackgroundState() {
        unloadKoinModules(backgroundModule)
    }

    @Test
    fun postsPaymentReminderWhenAppIsInBackground() {
        // GIVEN I am logged in and my payment is due today
        givenStoredSession(profile = allGoodProfile())

        // WHEN the reminder runs in the background
        val result = runReminderWorker()

        // THEN I am notified about the payment
        assertEquals(ListenableWorker.Result.success(), result)
        assertNotificationPosted(
            titleRes = R.string.payment_due_today_title,
            textRes = R.string.payment_due_today_text,
        )
    }

    @Test
    fun skipsPaymentReminderWhenNotificationsAreNotAllowed() {
        // GIVEN notifications are not allowed
        notificationsGranted = false
        givenStoredSession(profile = allGoodProfile())

        // WHEN the reminder runs in the background
        val result = runReminderWorker()

        // THEN the run still succeeds, only the notification is skipped
        assertEquals(ListenableWorker.Result.success(), result)
        assertNoNotificationPosted()
    }

    private fun runReminderWorker(): ListenableWorker.Result {
        val dueDate = allGoodProfile().nextPaymentDate.toString()
        return runBlocking {
            TestListenableWorkerBuilder<PaymentDueReminderWorker>(context)
                .setInputData(
                    workDataOf(
                        PaymentDueReminderSchedulerImpl.KEY_THRESHOLD to PaymentDueThreshold.DUE_DAY.name,
                        PaymentDueReminderSchedulerImpl.KEY_DUE_DATE to dueDate,
                    ),
                )
                .setWorkerFactory(KoinWorkerFactory())
                .build()
                .doWork()
        }
    }
}
