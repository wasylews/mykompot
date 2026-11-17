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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SessionKeepAliveSchedulerTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun scheduleEnqueuesUniqueKeepAliveWork() {
        val scheduler = SessionKeepAliveSchedulerImpl(workManager)

        scheduler.schedule()

        val infos = workManager.getWorkInfosForUniqueWork(SessionKeepAliveSchedulerImpl.KEEP_ALIVE_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
    }

    @Test
    fun scheduleReplacesExistingKeepAliveWork() {
        val scheduler = SessionKeepAliveSchedulerImpl(workManager)

        scheduler.schedule()
        val firstId = workManager.getWorkInfosForUniqueWork(SessionKeepAliveSchedulerImpl.KEEP_ALIVE_WORK_NAME).get().single().id

        scheduler.schedule()
        val infos = workManager.getWorkInfosForUniqueWork(SessionKeepAliveSchedulerImpl.KEEP_ALIVE_WORK_NAME).get()

        assertEquals(1, infos.size)
        val second = infos.single()
        assertEquals(WorkInfo.State.ENQUEUED, second.state)
        // REPLACE creates a new work request
        org.junit.Assert.assertNotEquals(firstId, second.id)
    }
}
