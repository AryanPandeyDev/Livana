package com.livana.app.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.MotionTokens
import com.livana.app.core.designsystem.theme.Radii
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.designsystem.theme.reducedMotionEnabled

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radii.Skeleton),
) {
    val reducedMotion = reducedMotionEnabled()
    val progress = if (reducedMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "livana-shimmer")
        val value by transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(MotionTokens.ShimmerMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "livana-shimmer-offset",
        )
        value
    }
    Canvas(modifier = modifier.clip(shape)) {
        drawRect(shimmerBrush(progress, reducedMotion))
    }
}

@Composable
fun StatCardSkeleton(modifier: Modifier = Modifier) {
    LivanaCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.S12)) {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth(0.46f)
                    .height(ComponentDimens.SkeletonStatHeight),
            )
            SkeletonBlock(
                Modifier
                    .fillMaxWidth(0.62f)
                    .height(ComponentDimens.SkeletonMetricHeight),
            )
            SkeletonBlock(
                Modifier
                    .fillMaxWidth(0.54f)
                    .height(ComponentDimens.SkeletonStatHeight),
            )
        }
    }
}

@Composable
fun PoolCardSkeleton(modifier: Modifier = Modifier) {
    LivanaCard(
        modifier = modifier.fillMaxWidth(),
        style = LivanaCardStyle.Media,
    ) {
        Column {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentDimens.SkeletonPoolImageHeight),
                shape = androidx.compose.ui.graphics.RectangleShape,
            )
            Column(
                modifier = Modifier.padding(Spacing.CompactCard),
                verticalArrangement = Arrangement.spacedBy(Spacing.S8),
            ) {
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth(0.64f)
                        .height(ComponentDimens.SkeletonTitleHeight),
                )
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth(0.9f)
                        .height(ComponentDimens.SkeletonCaptionHeight),
                )
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth()
                        .height(ComponentDimens.ProgressHeight),
                    shape = CircleShape,
                )
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth(0.44f)
                        .height(ComponentDimens.SkeletonCaptionHeight),
                )
            }
        }
    }
}

@Composable
fun ListRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.S12),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
    ) {
        SkeletonBlock(
            modifier = Modifier.size(ComponentDimens.SkeletonAvatarSize),
            shape = CircleShape,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.S8),
        ) {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(ComponentDimens.SkeletonStatHeight),
            )
            SkeletonBlock(
                Modifier
                    .fillMaxWidth(0.3f)
                    .height(ComponentDimens.SkeletonCaptionHeight),
            )
        }
    }
}

@Composable
fun DetailScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.S16),
    ) {
        SkeletonBlock(
            Modifier
                .fillMaxWidth()
                .height(ComponentDimens.SkeletonPoolImageHeight),
            shape = RoundedCornerShape(Radii.Image),
        )
        StatCardSkeleton()
        LivanaCard(style = LivanaCardStyle.Flat) {
            Column {
                repeat(3) { ListRowSkeleton() }
            }
        }
    }
}

private fun DrawScope.shimmerBrush(progress: Float, reducedMotion: Boolean): Brush {
    if (reducedMotion) return SolidColor(LivanaColors.SurfaceAlt)
    val centerX = size.width * progress
    return Brush.linearGradient(
        colorStops = arrayOf(
            0.25f to LivanaColors.SurfaceAlt,
            0.37f to LivanaColors.ShimmerHighlight,
            0.63f to LivanaColors.SurfaceAlt,
        ),
        start = Offset(centerX - size.width, 0f),
        end = Offset(centerX + size.width, size.height),
    )
}
