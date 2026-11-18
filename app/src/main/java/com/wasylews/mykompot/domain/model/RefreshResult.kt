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

/** Result of refreshing the authenticated profile/status from the portal. */
sealed interface RefreshResult {
    /** Profile data was fetched and stored successfully. */
    data class Updated(
        val profile: Profile,
    ) : RefreshResult

    /** The session is no longer valid (portal served a login page or no session was available). */
    data object Expired : RefreshResult

    /** Refresh failed without indicating session expiry. Cached data is left intact. */
    sealed interface Failed : RefreshResult {
        /** The request failed due to a connectivity or I/O problem. */
        data object Network : Failed

        /** The portal returned an unexpected, error, or unsuccessful response. */
        data object Server : Failed

        /** The response could not be parsed into a usable profile. */
        data object Parse : Failed
    }
}
