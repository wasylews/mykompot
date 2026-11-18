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

package com.wasylews.mykompot.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.wasylews.mykompot.domain.NotificationsPermission
import org.koin.core.annotation.Singleton

@Singleton
class AndroidNotificationsPermission(
    private val context: Context,
) : NotificationsPermission {
    override fun isGranted(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}
