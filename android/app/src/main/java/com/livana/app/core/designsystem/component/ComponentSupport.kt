package com.livana.app.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.MotionTokens
import com.livana.app.core.designsystem.theme.reducedMotionEnabled

internal enum class LivanaGlyphKind {
    Check,
    Star,
    Pause,
    Unlock,
    Clock,
    Pin,
    Copy,
    Alert,
    Empty,
    Offline,
}

@Composable
internal fun LivanaGlyph(
    kind: LivanaGlyphKind,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    size: Dp = ComponentDimens.SmallIconSize,
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeColor = if (color == Color.Unspecified) LivanaColors.Text else color
        val stroke = Stroke(
            width = this.size.minDimension * 0.085f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val w = this.size.width
        val h = this.size.height
        when (kind) {
            LivanaGlyphKind.Check -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.52f)
                    lineTo(w * 0.42f, h * 0.74f)
                    lineTo(w * 0.82f, h * 0.28f)
                }
                drawPath(path, strokeColor, style = stroke)
            }

            LivanaGlyphKind.Star -> {
                val path = Path()
                repeat(10) { index ->
                    val angle = Math.toRadians((-90.0 + index * 36.0))
                    val radius = if (index % 2 == 0) w * 0.4f else w * 0.18f
                    val point = Offset(
                        x = w / 2f + kotlin.math.cos(angle).toFloat() * radius,
                        y = h / 2f + kotlin.math.sin(angle).toFloat() * radius,
                    )
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                path.close()
                drawPath(path, strokeColor, style = stroke)
            }

            LivanaGlyphKind.Pause -> {
                drawLine(strokeColor, Offset(w * 0.36f, h * 0.25f), Offset(w * 0.36f, h * 0.75f), stroke.width, StrokeCap.Round)
                drawLine(strokeColor, Offset(w * 0.64f, h * 0.25f), Offset(w * 0.64f, h * 0.75f), stroke.width, StrokeCap.Round)
            }

            LivanaGlyphKind.Unlock -> {
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(w * 0.18f, h * 0.43f),
                    size = Size(w * 0.64f, h * 0.42f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke,
                )
                val path = Path().apply {
                    moveTo(w * 0.34f, h * 0.43f)
                    lineTo(w * 0.34f, h * 0.3f)
                    cubicTo(w * 0.34f, h * 0.08f, w * 0.72f, h * 0.08f, w * 0.72f, h * 0.3f)
                }
                drawPath(path, strokeColor, style = stroke)
            }

            LivanaGlyphKind.Clock -> {
                drawCircle(strokeColor, w * 0.38f, center, style = stroke)
                drawLine(strokeColor, center, Offset(w * 0.5f, h * 0.27f), stroke.width, StrokeCap.Round)
                drawLine(strokeColor, center, Offset(w * 0.67f, h * 0.6f), stroke.width, StrokeCap.Round)
            }

            LivanaGlyphKind.Pin -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.9f)
                    cubicTo(w * 0.22f, h * 0.62f, w * 0.2f, h * 0.18f, w * 0.5f, h * 0.12f)
                    cubicTo(w * 0.8f, h * 0.18f, w * 0.78f, h * 0.62f, w * 0.5f, h * 0.9f)
                }
                drawPath(path, strokeColor, style = stroke)
                drawCircle(strokeColor, w * 0.09f, Offset(w * 0.5f, h * 0.4f), style = stroke)
            }

            LivanaGlyphKind.Copy -> {
                drawRoundRect(
                    strokeColor,
                    topLeft = Offset(w * 0.36f, h * 0.34f),
                    size = Size(w * 0.48f, h * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke,
                )
                val path = Path().apply {
                    moveTo(w * 0.64f, h * 0.24f)
                    lineTo(w * 0.64f, h * 0.16f)
                    lineTo(w * 0.16f, h * 0.16f)
                    lineTo(w * 0.16f, h * 0.66f)
                    lineTo(w * 0.24f, h * 0.66f)
                }
                drawPath(path, strokeColor, style = stroke)
            }

            LivanaGlyphKind.Alert -> {
                drawCircle(strokeColor, w * 0.4f, center, style = stroke)
                drawLine(strokeColor, Offset(w * 0.5f, h * 0.29f), Offset(w * 0.5f, h * 0.56f), stroke.width, StrokeCap.Round)
                drawCircle(strokeColor, stroke.width / 2f, Offset(w * 0.5f, h * 0.7f))
            }

            LivanaGlyphKind.Empty -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    cubicTo(w * 0.68f, h * 0.34f, w * 0.82f, h * 0.4f, w * 0.82f, h * 0.62f)
                    cubicTo(w * 0.82f, h * 0.86f, w * 0.18f, h * 0.86f, w * 0.18f, h * 0.62f)
                    cubicTo(w * 0.18f, h * 0.4f, w * 0.32f, h * 0.34f, w * 0.5f, h * 0.12f)
                }
                drawPath(path, strokeColor, style = stroke)
                val smile = Path().apply {
                    moveTo(w * 0.34f, h * 0.62f)
                    quadraticTo(w * 0.5f, h * 0.72f, w * 0.66f, h * 0.62f)
                }
                drawPath(smile, strokeColor, style = stroke)
            }

            LivanaGlyphKind.Offline -> {
                val cloud = Path().apply {
                    moveTo(w * 0.18f, h * 0.68f)
                    cubicTo(w * 0.02f, h * 0.5f, w * 0.18f, h * 0.28f, w * 0.38f, h * 0.34f)
                    cubicTo(w * 0.52f, h * 0.12f, w * 0.82f, h * 0.22f, w * 0.84f, h * 0.48f)
                    cubicTo(w * 0.98f, h * 0.54f, w * 0.94f, h * 0.72f, w * 0.82f, h * 0.74f)
                }
                drawPath(cloud, strokeColor, style = stroke)
                drawLine(strokeColor, Offset(w * 0.14f, h * 0.14f), Offset(w * 0.86f, h * 0.86f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

@Composable
internal fun LivanaSpinner(
    modifier: Modifier = Modifier,
    color: Color = LivanaColors.Primary,
    trackColor: Color = LivanaColors.PrimaryContainer,
    size: Dp = ComponentDimens.SmallIconSize,
) {
    val reducedMotion = reducedMotionEnabled()
    val rotation = if (reducedMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "livana-spinner")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(MotionTokens.SpinnerMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "livana-spinner-angle",
        )
        angle
    }
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.11f
        drawCircle(trackColor, style = Stroke(strokeWidth))
        drawArc(
            color = color,
            startAngle = rotation - 90f,
            sweepAngle = if (reducedMotion) 90f else 110f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
    }
}
