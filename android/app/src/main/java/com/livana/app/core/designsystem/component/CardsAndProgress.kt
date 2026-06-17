package com.livana.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import com.livana.app.core.designsystem.theme.CardShape
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.Elevations
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing

enum class LivanaCardStyle {
    Standard,
    Flat,
    Media,
}

@Composable
fun LivanaCard(
    modifier: Modifier = Modifier,
    style: LivanaCardStyle = LivanaCardStyle.Standard,
    contentPadding: PaddingValues = PaddingValues(Spacing.Card),
    content: @Composable () -> Unit,
) {
    val elevation = if (style == LivanaCardStyle.Flat) Elevations.Small else Elevations.Standard
    val shadowColor = if (style == LivanaCardStyle.Flat) LivanaColors.ShadowSmall else LivanaColors.ShadowStandard
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = CardShape,
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .clip(CardShape)
            .background(LivanaColors.Surface, CardShape)
            .then(
                if (style == LivanaCardStyle.Media) {
                    Modifier
                } else {
                    Modifier.padding(contentPadding)
                },
            ),
    ) {
        content()
    }
}

@Composable
fun LivanaProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ComponentDimens.ProgressHeight),
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = LivanaColors.Border,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
        )
        if (clampedProgress > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(LivanaColors.GradProgressA, LivanaColors.GradProgressB),
                ),
                size = Size(size.width * clampedProgress, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            )
        }
    }
}
