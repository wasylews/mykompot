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

/** Result of fetching or loading more payments from the portal. */
sealed interface PaymentsActionResult {
    /** Payments were fetched and stored successfully. */
    data object Success : PaymentsActionResult

    /** The session is no longer valid (portal served a login page or no session was available). */
    data object Expired : PaymentsActionResult

    /** The operation failed without indicating session expiry. Cached payments are left intact. */
    data class Failed(
        val reason: PortalFailure,
    ) : PaymentsActionResult
}
