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

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wasylews.mykompot.domain.SessionKeepAliveScheduler
import org.koin.core.annotation.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class SessionKeepAliveSchedulerImpl(
    private val workManager: WorkManager,
) : SessionKeepAliveScheduler {
    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<SessionKeepAliveWorker>()
            .setInitialDelay(KEEP_ALIVE_DELAY_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        workManager.enqueueUniqueWork(
            KEEP_ALIVE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(KEEP_ALIVE_WORK_NAME)
    }

    companion object {
        const val KEEP_ALIVE_WORK_NAME = "session_keep_alive"
        const val KEEP_ALIVE_DELAY_HOURS = 12L
    }
}
