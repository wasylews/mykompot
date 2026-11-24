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
import okio.IOException
import retrofit2.HttpException

/** Maps expected portal transport failures, rethrowing anything else. */
internal fun Throwable.toPortalFailure(): PortalFailure = when (this) {
    is HttpException -> PortalFailure.Server
    is IOException -> PortalFailure.Network
    else -> throw this
}
