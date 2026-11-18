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

/** Result of an authentication attempt against the portal. */
sealed interface LoginResult {
    /** Login succeeded and the account [profile] was retrieved and stored. */
    data class Success(
        val profile: Profile,
    ) : LoginResult

    /** Credentials were rejected by the portal. */
    data object InvalidCredentials : LoginResult

    /** The request failed due to a connectivity or I/O problem. */
    data object NetworkError : LoginResult

    /** The portal returned an unexpected or unsuccessful response. */
    data object ServerError : LoginResult

    /** The response could not be parsed into a usable profile/session. */
    data object ParsingError : LoginResult
}
