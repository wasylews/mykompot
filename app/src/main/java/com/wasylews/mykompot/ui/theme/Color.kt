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
package com.wasylews.mykompot.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Light
val PrimaryLight = Color(0xFF6650E5)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE7E0FF)
val OnPrimaryContainerLight = Color(0xFF2A214C)

val SecondaryLight = Color(0xFF8F7BF0)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF3EFFD)
val OnSecondaryContainerLight = Color(0xFF322F3B)

val TertiaryLight = Color(0xFF2FB257)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFD7F3DD)
val OnTertiaryContainerLight = Color(0xFF0E5A2A)

val BackgroundLight = Color(0xFFF4F3FB)
val OnBackgroundLight = Color(0xFF1E1B28)
val SurfaceLight = Color(0xFFFBFAFF)
val OnSurfaceLight = Color(0xFF1E1B28)
val SurfaceVariantLight = Color(0xFFF3F2FA)
val OnSurfaceVariantLight = Color(0xFF49454F)
val OutlineLight = Color(0xFF797486)
val OutlineVariantLight = Color(0xFFCAC4D0)

val ErrorLight = Color(0xFFE34A3A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDDD4)
val OnErrorContainerLight = Color(0xFF8C2218)

val InverseSurfaceLight = Color(0xFF2A2740)
val InverseOnSurfaceLight = Color(0xFFFFFFFF)
val InversePrimaryLight = Color(0xFFCBBBFF)

// Dark
val PrimaryDark = Color(0xFFCBBBFF)
val OnPrimaryDark = Color(0xFF3A2C63)
val PrimaryContainerDark = Color(0xFF4C3A9D)
val OnPrimaryContainerDark = Color(0xFFEBE2FF)

val SecondaryDark = Color(0xFFCBBBFF)
val OnSecondaryDark = Color(0xFF3A2C63)
val SecondaryContainerDark = Color(0xFF433B5C)
val OnSecondaryContainerDark = Color(0xFFE0D9F2)

val TertiaryDark = Color(0xFF51D97C)
val OnTertiaryDark = Color(0xFF103520)
val TertiaryContainerDark = Color(0xFF103520)
val OnTertiaryContainerDark = Color(0xFFB9F7CA)

val BackgroundDark = Color(0xFF0E0D13)
val OnBackgroundDark = Color(0xFFE8E3F1)
val SurfaceDark = Color(0xFF1B1923)
val OnSurfaceDark = Color(0xFFE8E3F1)
val SurfaceVariantDark = Color(0xFF24212E)
val OnSurfaceVariantDark = Color(0xFFC4BFD0)
val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF3E3A4A)

val ErrorDark = Color(0xFFEF5B49)
val OnErrorDark = Color(0xFF3D1711)
val ErrorContainerDark = Color(0xFF3D1711)
val OnErrorContainerDark = Color(0xFFFFB8AC)

val InverseSurfaceDark = Color(0xFF35313F)
val InverseOnSurfaceDark = Color(0xFF3A2C63)
val InversePrimaryDark = Color(0xFF6650E5)

// State colors
data class StatusColors(
    val container: Color,
    val on: Color,
    val accent: Color,
)

val StatusColorsLight =
    StatusColors(
        container = Color(0xFFD7F3DD),
        on = Color(0xFF0E5A2A),
        accent = Color(0xFF2FB257),
    )

val StatusColorsDark =
    StatusColors(
        container = Color(0xFF103520),
        on = Color(0xFFB9F7CA),
        accent = Color(0xFF51D97C),
    )

val LocalStatusColors = compositionLocalOf { StatusColorsLight }
