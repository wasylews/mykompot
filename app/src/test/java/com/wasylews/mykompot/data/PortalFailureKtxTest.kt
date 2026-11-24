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

import com.wasylews.mykompot.domain.model.PortalFailure
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PortalFailureKtxTest {
    @Test
    fun `maps HTTP exceptions to server failures`() {
        val response = Response.error<Any>(
            500,
            "Server error".toResponseBody("text/plain".toMediaType()),
        )

        assertEquals(PortalFailure.Server, HttpException(response).toPortalFailure())
    }

    @Test
    fun `maps IO exceptions to network failures`() {
        assertEquals(PortalFailure.Network, IOException().toPortalFailure())
    }

    @Test
    fun `rethrows unexpected failures`() {
        val failure = IllegalStateException("Unexpected")

        val actual = runCatching { failure.toPortalFailure() }.exceptionOrNull()

        assertSame(failure, actual)
    }
}
