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
 * Tariff plan details.
 *
 * @property id stable plan identifier (for portal-derived plans typically like `300GRN_100mb`).
 * @property name human-readable plan name.
 * @property speed plan speed in Mbps.
 * @property price monthly plan price in UAH.
 */
data class TariffPlan(
    val id: String,
    val name: String,
    val speed: Int,
    val price: Int,
)
