package com.livana.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.livana.app.core.designsystem.theme.ComponentDimens

private const val IconStrokeRatio = 0.1f
private const val CheckStrokeRatio = 0.15f

@Composable
fun BackChevronIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawLine(tint, Offset(size.width * 0.68f, size.height * 0.18f), Offset(size.width * 0.32f, size.height * 0.5f), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.32f, size.height * 0.5f), Offset(size.width * 0.68f, size.height * 0.82f), stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun ShareIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawLine(tint, Offset(size.width * 0.5f, size.height * 0.18f), Offset(size.width * 0.5f, size.height * 0.68f), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.28f, size.height * 0.38f), Offset(size.width * 0.5f, size.height * 0.18f), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.72f, size.height * 0.38f), Offset(size.width * 0.5f, size.height * 0.18f), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.22f, size.height * 0.74f), Offset(size.width * 0.22f, size.height * 0.9f), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.22f, size.height * 0.9f), Offset(size.width * 0.78f, size.height * 0.9f), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.78f, size.height * 0.9f), Offset(size.width * 0.78f, size.height * 0.74f), stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun HeartIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.84f)
            cubicTo(size.width * 0.12f, size.height * 0.58f, size.width * 0.16f, size.height * 0.22f, size.width * 0.38f, size.height * 0.28f)
            cubicTo(size.width * 0.48f, size.height * 0.3f, size.width * 0.5f, size.height * 0.42f, size.width * 0.5f, size.height * 0.42f)
            cubicTo(size.width * 0.5f, size.height * 0.42f, size.width * 0.54f, size.height * 0.3f, size.width * 0.66f, size.height * 0.28f)
            cubicTo(size.width * 0.88f, size.height * 0.24f, size.width * 0.92f, size.height * 0.58f, size.width * 0.5f, size.height * 0.84f)
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun CheckIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.StatusIconSize)) {
        val strokeWidth = size.minDimension * CheckStrokeRatio
        drawLine(tint, Offset(size.width * 0.18f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.75f), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.42f, size.height * 0.75f), Offset(size.width * 0.84f, size.height * 0.25f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun ChevronRightIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.SmallIconSize)) {
        val strokeWidth = size.minDimension * IconStrokeRatio
        drawLine(tint, Offset(size.width * 0.34f, size.height * 0.18f), Offset(size.width * 0.68f, size.height * 0.5f), strokeWidth, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.68f, size.height * 0.5f), Offset(size.width * 0.34f, size.height * 0.82f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun ShieldIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.12f)
            lineTo(size.width * 0.82f, size.height * 0.26f)
            lineTo(size.width * 0.82f, size.height * 0.48f)
            cubicTo(size.width * 0.82f, size.height * 0.72f, size.width * 0.65f, size.height * 0.86f, size.width * 0.5f, size.height * 0.92f)
            cubicTo(size.width * 0.35f, size.height * 0.86f, size.width * 0.18f, size.height * 0.72f, size.width * 0.18f, size.height * 0.48f)
            lineTo(size.width * 0.18f, size.height * 0.26f)
            close()
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun DocumentIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val path = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.1f)
            lineTo(size.width * 0.62f, size.height * 0.1f)
            lineTo(size.width * 0.8f, size.height * 0.28f)
            lineTo(size.width * 0.8f, size.height * 0.9f)
            lineTo(size.width * 0.28f, size.height * 0.9f)
            close()
            moveTo(size.width * 0.62f, size.height * 0.1f)
            lineTo(size.width * 0.62f, size.height * 0.28f)
            lineTo(size.width * 0.8f, size.height * 0.28f)
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun MedalIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        // Circle (medal head)
        drawCircle(tint, radius = size.width * 0.22f, center = Offset(size.width * 0.5f, size.height * 0.36f), style = stroke)
        // Ribbon
        val path = Path().apply {
            moveTo(size.width * 0.36f, size.height * 0.54f)
            lineTo(size.width * 0.27f, size.height * 0.88f)
            lineTo(size.width * 0.5f, size.height * 0.74f)
            lineTo(size.width * 0.73f, size.height * 0.88f)
            lineTo(size.width * 0.64f, size.height * 0.54f)
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun ShieldCheckIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val shield = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.08f)
            lineTo(size.width * 0.85f, size.height * 0.22f)
            lineTo(size.width * 0.85f, size.height * 0.44f)
            cubicTo(size.width * 0.85f, size.height * 0.7f, size.width * 0.7f, size.height * 0.82f, size.width * 0.5f, size.height * 0.92f)
            cubicTo(size.width * 0.3f, size.height * 0.82f, size.width * 0.15f, size.height * 0.7f, size.width * 0.15f, size.height * 0.44f)
            lineTo(size.width * 0.15f, size.height * 0.22f)
            close()
        }
        drawPath(shield, tint, style = stroke)
        // Checkmark inside
        drawLine(tint, Offset(size.width * 0.35f, size.height * 0.5f), Offset(size.width * 0.45f, size.height * 0.6f), stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.45f, size.height * 0.6f), Offset(size.width * 0.65f, size.height * 0.36f), stroke.width, cap = StrokeCap.Round)
    }
}
