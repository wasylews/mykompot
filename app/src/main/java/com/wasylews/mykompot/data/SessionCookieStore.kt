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

import kotlinx.coroutines.flow.Flow

/**
 * Synchronous storage for the portal session cookie.
 *
 * OkHttp reads cookies on dispatcher threads, so the jar needs a snapshot it can read without
 * suspending. The flow mirrors the stored value for session observation.
 */
interface SessionCookieStore {
    /** Current `sid` cookie value, or `null` when there is no session. */
    fun sid(): String?

    /** Persists the `sid` cookie value received from the portal. */
    fun saveSid(sid: String)

    /** Removes the stored session cookie. */
    fun clear()

    /** Emits the stored `sid`, or `null` when absent. */
    fun sidFlow(): Flow<String?>
}
