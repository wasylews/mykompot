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

import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class FakeCall<T>(
    private val response: () -> Response<T>,
    private val exception: Exception? = null,
) : Call<T> {
    override fun execute(): Response<T> {
        if (exception != null) throw exception
        return response()
    }

    override fun enqueue(callback: Callback<T>) {
        runCatching { execute() }
            .onSuccess { callback.onResponse(this, it) }
            .onFailure { callback.onFailure(this, it) }
    }

    override fun isExecuted(): Boolean = false

    override fun cancel() = Unit

    override fun isCanceled(): Boolean = false

    override fun clone(): Call<T> = this

    override fun request(): Request = Request.Builder().url("http://127.0.0.1:8080/index.cgi").build()

    override fun timeout(): Timeout = Timeout()
}
