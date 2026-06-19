package com.livana.app.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.MetricStyles
import com.livana.app.core.designsystem.theme.Spacing

/** Three-up platform-stat tiles (active pools / verified NGOs / total pools). */
@Composable
internal fun HomeStatTiles(
    activePools: Long,
    verifiedNgos: Long,
    totalPools: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
    ) {
        StatTile(
            value = activePools.toString(),
            label = "Active pools",
            tint = IconChipTint.Jade,
            icon = HomeTileIcon.Pools,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = verifiedNgos.toString(),
            label = "Verified NGOs",
            tint = IconChipTint.Coral,
            icon = HomeTileIcon.Verified,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = totalPools.toString(),
            label = "Total pools",
            tint = IconChipTint.Info,
            icon = HomeTileIcon.Clock,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    tint: IconChipTint,
    icon: HomeTileIcon,
    modifier: Modifier = Modifier,
) {
    LivanaCard(
        modifier = modifier,
        style = LivanaCardStyle.Flat,
        contentPadding = PaddingValues(Spacing.S16),
    ) {
        Column {
            IconChip(
                tint = tint,
                modifier = Modifier.size(ComponentDimens.IconButtonVisualSize),
            ) {
                HomeTileGlyph(icon = icon)
            }
            Text(
                text = value,
                modifier = Modifier.padding(top = Spacing.S12),
                color = LivanaColors.Text,
                style = MetricStyles.Display,
            )
            Text(
                text = label,
                modifier = Modifier.padding(top = Spacing.S4),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun HomeTileGlyph(icon: HomeTileIcon) {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * 0.1f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val w = size.width
        val h = size.height
        when (icon) {
            HomeTileIcon.Pools -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.24f),
                    size = Size(w * 0.76f, h * 0.58f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.12f, h * 0.43f), Offset(w * 0.88f, h * 0.43f), stroke.width)
            }

            HomeTileIcon.Verified -> {
                val shield = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.82f, h * 0.26f)
                    lineTo(w * 0.82f, h * 0.5f)
                    cubicTo(w * 0.82f, h * 0.72f, w * 0.66f, h * 0.84f, w * 0.5f, h * 0.9f)
                    cubicTo(w * 0.34f, h * 0.84f, w * 0.18f, h * 0.72f, w * 0.18f, h * 0.5f)
                    lineTo(w * 0.18f, h * 0.26f)
                    close()
                }
                drawPath(shield, color, style = stroke)
                drawLine(color, Offset(w * 0.36f, h * 0.53f), Offset(w * 0.47f, h * 0.64f), stroke.width)
                drawLine(color, Offset(w * 0.47f, h * 0.64f), Offset(w * 0.66f, h * 0.4f), stroke.width)
            }

            HomeTileIcon.Clock -> {
                drawCircle(color, radius = w * 0.38f, style = stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.5f, h * 0.52f), stroke.width)
                drawLine(color, Offset(w * 0.5f, h * 0.52f), Offset(w * 0.66f, h * 0.66f), stroke.width)
            }
        }
    }
}

private enum class HomeTileIcon {
    Pools,
    Verified,
    Clock,
}
