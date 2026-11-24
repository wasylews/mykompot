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

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wasylews.mykompot.domain.PaymentDueReminderScheduler
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.annotation.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class MyKompotApplication : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin<MyKompotApplication> {
            androidContext(this@MyKompotApplication)
            workManagerFactory()
        }

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            get<PaymentDueReminderScheduler>().start()
        }
    }
}
