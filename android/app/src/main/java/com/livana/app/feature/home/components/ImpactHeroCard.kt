package com.livana.app.feature.home.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import com.livana.app.core.designsystem.theme.CardShape
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.Elevations
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.MetricStyles
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.Usdc

/** Gradient impact hero card showing total donated + released (02-home.html). */
@Composable
internal fun ImpactHeroCard(
    totalDonated: Usdc,
    totalReleased: Usdc,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = Elevations.Jade,
                shape = CardShape,
                ambientColor = LivanaColors.ShadowJade,
                spotColor = LivanaColors.ShadowJade,
            )
            .clip(CardShape)
            .background(
                Brush.linearGradient(
                    listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroB, LivanaColors.GradHeroC),
                ),
            )
            .drawBehind {
                drawCircle(
                    color = LivanaColors.HeroHighlight,
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.92f, -size.height * 0.12f),
                )
                drawCircle(
                    color = LivanaColors.OnPrimary.copy(alpha = 0.13f),
                    radius = size.width * 0.18f,
                    center = Offset(size.width * 1.03f, -size.height * 0.1f),
                )
            }
            .padding(Spacing.Card),
    ) {
        Column {
            Text(
                text = "Total donated on Livana",
                color = LivanaColors.OnPrimary.copy(alpha = 0.82f),
                style = ComponentTextStyles.Pill.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = totalDonated.formatWhole(),
                modifier = Modifier.padding(top = Spacing.S8),
                color = LivanaColors.OnPrimary,
                style = MetricStyles.Large,
            )
            Row(
                modifier = Modifier.padding(top = Spacing.S12),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${totalReleased.formatWhole()} released to causes",
                    color = LivanaColors.OnPrimary.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelLarge,
                )
                VerifiablePill()
            }
        }
    }
}

@Composable
private fun VerifiablePill() {
    Row(
        modifier = Modifier
            .height(ComponentDimens.PillHeight)
            .background(LivanaColors.OnPrimary.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = ComponentDimens.PillHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.PillContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(ComponentDimens.StatusIconSize)) {
            val stroke = Stroke(
                width = size.minDimension * 0.16f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            drawLine(
                color = LivanaColors.OnPrimary,
                start = Offset(size.width * 0.18f, size.height * 0.52f),
                end = Offset(size.width * 0.42f, size.height * 0.76f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = LivanaColors.OnPrimary,
                start = Offset(size.width * 0.42f, size.height * 0.76f),
                end = Offset(size.width * 0.84f, size.height * 0.24f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = "verifiable",
            color = LivanaColors.OnPrimary,
            style = ComponentTextStyles.Pill,
        )
    }
}
