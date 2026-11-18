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

package com.wasylews.mykompot.domain.model

/** Result of refreshing the tariff plans catalog. */
sealed interface TariffPlansActionResult {
    /** Plans were fetched and stored successfully. */
    data object Success : TariffPlansActionResult

    /** The remote catalog has not changed since the cached version (HTTP 304). */
    data object NotModified : TariffPlansActionResult

    /** Refresh failed. Cached plans are left intact. */
    sealed interface Failed : TariffPlansActionResult {
        /** The request failed due to a connectivity or I/O problem. */
        data object Network : Failed

        /** The portal/server returned an unsuccessful response. */
        data object Server : Failed

        /** The response could not be parsed into a usable plans catalog. */
        data object Parse : Failed
    }
}
