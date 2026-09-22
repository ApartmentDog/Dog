package com.evyr.rads.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class AppIcon {
    LOG, STATS, SYNC, SETUP, PERSON, CHEVRON_RIGHT, CHEVRON_DOWN,
    SEARCH, BARCODE, CAMERA, PHOTO, PENCIL
}

/**
 * Line icons drawn on a 24-unit grid, traced from the Tabler icons used in
 * the approved mockup (clipboard-list, chart-bar, refresh, settings, user,
 * chevrons). Drawn by hand so the app doesn't need material-icons-extended,
 * which is where the chart/sync glyphs live.
 */
@Composable
fun AppIconView(
    icon: AppIcon,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    strokeWidth: Dp = 1.8.dp
) {
    Canvas(modifier.size(iconSize)) {
        val s = this.size.minDimension / 24f
        val sw = strokeWidth.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun pt(x: Float, y: Float) = Offset(x * s, y * s)
        fun box(l: Float, t: Float, r: Float, b: Float) = Rect(l * s, t * s, r * s, b * s)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, pt(x1, y1), pt(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
        fun roundBox(l: Float, t: Float, r: Float, b: Float, radius: Float) =
            drawRoundRect(
                color = color,
                topLeft = pt(l, t),
                size = Size((r - l) * s, (b - t) * s),
                cornerRadius = CornerRadius(radius * s),
                style = stroke
            )

        when (icon) {
            AppIcon.LOG -> {
                // Clipboard body, open at the top where the clip sits.
                val body = Path().apply {
                    moveTo(9f * s, 5f * s)
                    lineTo(7f * s, 5f * s)
                    arcTo(box(5f, 5f, 9f, 9f), -90f, -90f, false)
                    lineTo(5f * s, 19f * s)
                    arcTo(box(5f, 17f, 9f, 21f), 180f, -90f, false)
                    lineTo(17f * s, 21f * s)
                    arcTo(box(15f, 17f, 19f, 21f), 90f, -90f, false)
                    lineTo(19f * s, 7f * s)
                    arcTo(box(15f, 5f, 19f, 9f), 0f, -90f, false)
                    lineTo(15f * s, 5f * s)
                }
                drawPath(body, color, style = stroke)
                roundBox(9f, 3f, 15f, 7f, 2f)
                drawCircle(color, radius = sw * 0.6f, center = pt(9f, 12f))
                drawCircle(color, radius = sw * 0.6f, center = pt(9f, 16f))
                line(13f, 12f, 15f, 12f)
                line(13f, 16f, 15f, 16f)
            }
            AppIcon.STATS -> {
                roundBox(3f, 12f, 9f, 20f, 1f)
                roundBox(9f, 4f, 15f, 20f, 1f)
                roundBox(15f, 8f, 21f, 20f, 1f)
            }
            AppIcon.SYNC -> {
                drawArc(
                    color = color, startAngle = -7f, sweepAngle = -151f, useCenter = false,
                    topLeft = pt(4f, 4f), size = Size(16f * s, 16f * s), style = stroke
                )
                line(4f, 5f, 4f, 9f)
                line(4f, 9f, 8f, 9f)
                drawArc(
                    color = color, startAngle = 173f, sweepAngle = -151f, useCenter = false,
                    topLeft = pt(4f, 4f), size = Size(16f * s, 16f * s), style = stroke
                )
                line(20f, 19f, 20f, 15f)
                line(20f, 15f, 16f, 15f)
            }
            AppIcon.SETUP -> {
                // Eight-tooth gear: each tooth rises from the inner radius to
                // the outer and back, joined by short chords across valleys.
                fun polar(deg: Float, radius: Float): Offset {
                    val rad = Math.toRadians(deg.toDouble())
                    return Offset(
                        ((12.0 + radius * cos(rad)) * s).toFloat(),
                        ((12.0 + radius * sin(rad)) * s).toFloat()
                    )
                }
                val gear = Path()
                for (i in 0 until 8) {
                    val a = i * 45f
                    val p1 = polar(a - 15f, 7.2f)
                    val p2 = polar(a - 8f, 9.5f)
                    val p3 = polar(a + 8f, 9.5f)
                    val p4 = polar(a + 15f, 7.2f)
                    if (i == 0) gear.moveTo(p1.x, p1.y) else gear.lineTo(p1.x, p1.y)
                    gear.lineTo(p2.x, p2.y)
                    gear.lineTo(p3.x, p3.y)
                    gear.lineTo(p4.x, p4.y)
                }
                gear.close()
                drawPath(gear, color, style = stroke)
                drawCircle(color, radius = 3f * s, center = pt(12f, 12f), style = stroke)
            }
            AppIcon.PERSON -> {
                drawCircle(color, radius = 4f * s, center = pt(12f, 8f), style = stroke)
                drawArc(
                    color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = pt(6f, 15f), size = Size(12f * s, 12f * s), style = stroke
                )
            }
            AppIcon.CHEVRON_RIGHT -> {
                line(9f, 6f, 15f, 12f)
                line(15f, 12f, 9f, 18f)
            }
            AppIcon.CHEVRON_DOWN -> {
                line(6f, 9f, 12f, 15f)
                line(12f, 15f, 18f, 9f)
            }
            AppIcon.SEARCH -> {
                drawCircle(color, radius = 6.5f * s, center = pt(10.5f, 10.5f), style = stroke)
                line(15.5f, 15.5f, 20f, 20f)
            }
            AppIcon.BARCODE -> {
                // Scanner corner brackets around a few bars.
                line(4f, 8f, 4f, 4f); line(4f, 4f, 8f, 4f)
                line(16f, 4f, 20f, 4f); line(20f, 4f, 20f, 8f)
                line(4f, 16f, 4f, 20f); line(4f, 20f, 8f, 20f)
                line(16f, 20f, 20f, 20f); line(20f, 20f, 20f, 16f)
                line(8f, 9f, 8f, 15f)
                line(11f, 9f, 11f, 15f)
                line(13.5f, 9f, 13.5f, 15f)
                line(16f, 9f, 16f, 15f)
            }
            AppIcon.CAMERA -> {
                roundBox(3f, 7f, 21f, 20f, 2f)
                line(8.5f, 7f, 10f, 4.5f)
                line(10f, 4.5f, 14f, 4.5f)
                line(14f, 4.5f, 15.5f, 7f)
                drawCircle(color, radius = 3.5f * s, center = pt(12f, 13.5f), style = stroke)
            }
            AppIcon.PHOTO -> {
                roundBox(3f, 3f, 21f, 21f, 3f)
                drawCircle(color, radius = sw * 0.7f, center = pt(15.5f, 8.5f))
                line(3f, 16f, 8f, 11f); line(8f, 11f, 13f, 16f)
                line(13f, 15f, 16f, 12f); line(16f, 12f, 21f, 17f)
            }
            AppIcon.PENCIL -> {
                val pencil = Path().apply {
                    moveTo(4f * s, 20f * s)
                    lineTo(4f * s, 16f * s)
                    lineTo(15f * s, 5f * s)
                    lineTo(19f * s, 9f * s)
                    lineTo(8f * s, 20f * s)
                    close()
                }
                drawPath(pencil, color, style = stroke)
                line(13f, 7f, 17f, 11f)
            }
        }
    }
}
