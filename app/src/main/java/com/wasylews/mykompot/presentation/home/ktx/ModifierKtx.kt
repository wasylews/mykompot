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

package com.wasylews.mykompot.presentation.home.ktx

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Draws the outer edges of a card shared by adjacent lazy items.
 *
 * Compose has no stock modifier that outlines several siblings as one card, so every row draws its
 * own share of the frame: the side edges always, plus the top or the bottom edge and its arcs on the
 * first and the last row. Seams between rows therefore stay a single [stroke] wide instead of
 * doubling up under two full [Modifier.border] outlines.
 *
 * Pass the same [corner] that shapes the row backgrounds — the arcs are drawn from this value, so a
 * mismatch would leave the stroke visibly offset from the rounded background it is supposed to
 * follow.
 */
internal fun Modifier.groupedCardBorder(
    isFirst: Boolean,
    isLast: Boolean,
    corner: Dp,
    stroke: Dp,
    color: Color,
): Modifier = drawBehind {
    val strokePx = stroke.toPx()
    val half = strokePx / 2f
    val radius = corner.toPx()
    val left = half
    val right = size.width - half
    val top = half
    val bottom = size.height - half
    val topInset = if (isFirst) radius else 0f
    val bottomInset = if (isLast) radius else 0f

    drawLine(color, Offset(left, top + topInset), Offset(left, bottom - bottomInset), strokePx)
    drawLine(color, Offset(right, top + topInset), Offset(right, bottom - bottomInset), strokePx)

    if (isFirst) {
        drawLine(color, Offset(left + radius, top), Offset(right - radius, top), strokePx)
        drawCorner(color, stroke, corner, topLeft = Offset(left, top), startAngle = 180f)
        drawCorner(color, stroke, corner, topLeft = Offset(right - radius * 2, top), startAngle = 270f)
    }
    if (isLast) {
        drawLine(color, Offset(left + radius, bottom), Offset(right - radius, bottom), strokePx)
        drawCorner(
            color = color,
            stroke = stroke,
            corner = corner,
            topLeft = Offset(right - radius * 2, bottom - radius * 2),
            startAngle = 0f,
        )
        drawCorner(
            color = color,
            stroke = stroke,
            corner = corner,
            topLeft = Offset(left, bottom - radius * 2),
            startAngle = 90f,
        )
    }
}

private fun DrawScope.drawCorner(
    color: Color,
    stroke: Dp,
    corner: Dp,
    topLeft: Offset,
    startAngle: Float,
) {
    val radius = corner.toPx()
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = topLeft,
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke.toPx()),
    )
}
