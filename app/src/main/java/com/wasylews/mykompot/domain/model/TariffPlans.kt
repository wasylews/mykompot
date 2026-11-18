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

/**
 * Cached catalog of available tariff plans.
 *
 * @property plans available plans.
 * @property etag HTTP ETag validator from the last successful plans fetch, used for conditional
 * refresh.
 * @property lastModified HTTP Last-Modified validator from the last successful plans fetch, used for
 * conditional refresh.
 */
data class TariffPlans(
    val plans: List<TariffPlan>,
    val etag: String? = null,
    val lastModified: String? = null,
)
