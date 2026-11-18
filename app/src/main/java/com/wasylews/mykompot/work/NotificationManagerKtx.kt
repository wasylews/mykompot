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

import android.app.NotificationManager

/**
 * Whether this app may post notifications to [channelId].
 *
 * The caller is expected to create the channel before checking it, so a channel the user disabled is
 * detected while a missing channel is treated as enabled.
 */
internal fun NotificationManager.canPostTo(channelId: String): Boolean =
    getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
