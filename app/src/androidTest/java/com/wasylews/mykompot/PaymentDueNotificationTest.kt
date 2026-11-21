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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.wasylews.mykompot.ProfileFixtures
import com.wasylews.mykompot.domain.AppForegroundState
import com.wasylews.mykompot.domain.PaymentDueNotifiedStore
import com.wasylews.mykompot.domain.model.PaymentDueReminder
import com.wasylews.mykompot.domain.model.PaymentDueThreshold
import com.wasylews.mykompot.work.PaymentDueReminderWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.get
import java.time.LocalDate
import java.time.ZoneId

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PaymentDueNotificationTest : BaseAcceptanceTest() {
    private var appInForeground = false

    private val testModule = module {
        single<AppForegroundState> {
            object : AppForegroundState {
                override fun isInForeground(): Boolean = appInForeground
            }
        }
        single<ZoneId> { ZoneId.of("UTC") }
    }

    @Before
    fun setUpEnvironment() {
        // Channel importance is sticky even after deletion; start every test from a clean slate.
        notificationManager().deleteNotificationChannel(PAYMENT_CHANNEL_ID)
        loadKoinModules(testModule)
    }

    @After
    fun tearDownEnvironment() {
        unloadKoinModules(testModule)
        notificationManager().deleteNotificationChannel(PAYMENT_CHANNEL_ID)
    }

    @Test
    fun postsThreeDayReminderWhenAppIsInBackground() {
        givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = TODAY.plusDays(3)))

        assertEquals(ListenableWorker.Result.success(), runReminderWorker())

        assertNotificationPosted(
            titleRes = R.string.payment_due_in_three_days_title,
            textRes = R.string.payment_due_in_three_days_text,
        )
        assertLedgerContains(reminderKey(PaymentDueThreshold.THREE_DAYS_BEFORE, TODAY.plusDays(3)))
    }

    @Test
    fun postsDueDayReminderWhenAppIsInBackground() {
        givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = TODAY))

        assertEquals(ListenableWorker.Result.success(), runReminderWorker())

        assertNotificationPosted(
            titleRes = R.string.payment_due_today_title,
            textRes = R.string.payment_due_today_text,
        )
        assertLedgerContains(reminderKey(PaymentDueThreshold.DUE_DAY, TODAY))
    }

    @Test
    fun skipsOneDayBeforeAndOverdueReminders() {
        listOf(TODAY.plusDays(1), TODAY.minusDays(1)).forEach { dueDate ->
            givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = dueDate))

            assertEquals(ListenableWorker.Result.success(), runReminderWorker())
        }

        assertNoNotificationPosted()
        assertLedgerEmpty()
    }

    @Test
    fun doesNotRepostReminderAfterItWasMarked() {
        givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = TODAY))

        runReminderWorker()
        runReminderWorker()

        assertEquals(1, notificationManager().activeNotifications.size)
        assertLedgerContains(reminderKey(PaymentDueThreshold.DUE_DAY, TODAY))
    }

    @Test
    fun notifiesAgainForANewDueDate() {
        givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = TODAY))
        runReminderWorker()

        givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = TODAY.plusDays(3)))
        runReminderWorker()

        assertLedgerContains(reminderKey(PaymentDueThreshold.DUE_DAY, TODAY))
        assertLedgerContains(reminderKey(PaymentDueThreshold.THREE_DAYS_BEFORE, TODAY.plusDays(3)))
        assertEquals(2, notificationManager().activeNotifications.size)
    }

    @Test
    fun skipsReminderWhenAppIsInForeground() {
        appInForeground = true
        givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = TODAY))

        assertEquals(ListenableWorker.Result.success(), runReminderWorker())

        assertNoNotificationPosted()
        assertLedgerEmpty()
    }

    @Test
    fun skipsReminderWhenNotificationsAreNotAllowed() {
        notificationsGranted = false
        givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = TODAY))

        assertEquals(ListenableWorker.Result.success(), runReminderWorker())

        assertNoNotificationPosted()
        assertLedgerEmpty()
    }

    @Test
    fun zSkipsReminderWhenChannelIsDisabled() {
        notificationManager().createNotificationChannel(
            NotificationChannel(PAYMENT_CHANNEL_ID, "disabled", NotificationManager.IMPORTANCE_NONE),
        )
        givenStoredSession(profile = ProfileFixtures.profile(nextPaymentDate = TODAY))

        assertEquals(ListenableWorker.Result.success(), runReminderWorker())

        assertNoNotificationPosted()
        assertLedgerEmpty()
    }

    private fun runReminderWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<PaymentDueReminderWorker>(context)
            .setWorkerFactory(KoinWorkerFactory())
            .build()
            .doWork()
    }

    private fun notificationManager(): NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun assertLedgerContains(key: String) = runBlocking {
        assertTrue(get<PaymentDueNotifiedStore>().isNotified(key))
    }

    private fun assertLedgerEmpty() = runBlocking {
        assertFalse(get<PaymentDueNotifiedStore>().isNotified(reminderKey(PaymentDueThreshold.DUE_DAY, TODAY)))
        assertFalse(
            get<PaymentDueNotifiedStore>().isNotified(
                reminderKey(PaymentDueThreshold.THREE_DAYS_BEFORE, TODAY.plusDays(3)),
            ),
        )
    }

    private fun reminderKey(
        threshold: PaymentDueThreshold,
        dueDate: LocalDate,
    ): String = PaymentDueReminder(threshold, dueDate).ledgerKey

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 10, 1)
        const val PAYMENT_CHANNEL_ID = "payment_reminders"
    }
}
