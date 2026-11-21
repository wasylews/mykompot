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
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeSessionCookieStore(
    sid: String? = null,
) : SessionCookieStore {
    private val sid = MutableStateFlow(sid)

    override fun sid(): String? = sid.value

    override fun saveSid(sid: String) {
        this.sid.value = sid
    }

    override fun clear() {
        sid.value = null
    }

    override fun sidFlow(): Flow<String?> = sid
}
