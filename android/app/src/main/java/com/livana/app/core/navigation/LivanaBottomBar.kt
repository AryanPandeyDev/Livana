package com.livana.app.core.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.Elevations
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.Spacing

data class LivanaTopLevelTab(
    val destination: Destination,
    val label: String,
    val icon: LivanaBottomBarIcon,
)

enum class LivanaBottomBarIcon {
    Home,
    Explore,
    Boards,
    Activity,
    Profile,
}

val LivanaTopLevelTabs = listOf(
    LivanaTopLevelTab(Destination.Home, "Home", LivanaBottomBarIcon.Home),
    LivanaTopLevelTab(Destination.Explore, "Explore", LivanaBottomBarIcon.Explore),
    LivanaTopLevelTab(Destination.Boards, "Boards", LivanaBottomBarIcon.Boards),
    LivanaTopLevelTab(Destination.Activity, "Activity", LivanaBottomBarIcon.Activity),
    LivanaTopLevelTab(Destination.Profile, "Profile", LivanaBottomBarIcon.Profile),
)

@Composable
fun LivanaBottomBar(
    selectedDestination: Destination,
    onDestinationSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = Elevations.Small,
                ambientColor = LivanaColors.ShadowSmall,
                spotColor = LivanaColors.ShadowSmall,
            )
            .background(LivanaColors.Surface)
            .drawBehind {
                drawLine(
                    color = LivanaColors.Hairline,
                    start = Offset.Zero,
                    end = Offset(size.width, Offset.Zero.y),
                    strokeWidth = Borders.Hairline.toPx(),
                )
            }
            .navigationBarsPadding()
            .height(ComponentDimens.MinimumTouchTarget + Spacing.S24)
            .padding(horizontal = Spacing.S8, vertical = Spacing.S12),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Top,
    ) {
        LivanaTopLevelTabs.forEach { tab ->
            val selected = tab.destination.route == selectedDestination.route
            LivanaBottomBarItem(
                tab = tab,
                selected = selected,
                onClick = { onDestinationSelected(tab.destination) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LivanaBottomBarItem(
    tab: LivanaTopLevelTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) LivanaColors.Primary else LivanaColors.TextMuted
    Column(
        modifier = modifier
            .height(ComponentDimens.MinimumTouchTarget)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .semantics {
                contentDescription = tab.label
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.S4),
    ) {
        LivanaBottomBarGlyph(
            icon = tab.icon,
            color = contentColor,
            modifier = Modifier.size(ComponentDimens.BrandMarkSize),
        )
        Text(
            text = tab.label,
            color = contentColor,
            style = ComponentTextStyles.Pill.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun LivanaBottomBarGlyph(
    icon: LivanaBottomBarIcon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = size.minDimension * 0.075f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val w = size.width
        val h = size.height
        when (icon) {
            LivanaBottomBarIcon.Home -> {
                val roof = Path().apply {
                    moveTo(w * 0.16f, h * 0.48f)
                    lineTo(w * 0.5f, h * 0.18f)
                    lineTo(w * 0.84f, h * 0.48f)
                }
                drawPath(roof, color, style = stroke)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.25f, h * 0.46f),
                    size = Size(w * 0.5f, h * 0.38f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke,
                )
            }

            LivanaBottomBarIcon.Explore -> {
                drawCircle(
                    color = color,
                    radius = w * 0.32f,
                    center = Offset(w * 0.48f, h * 0.46f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.7f, h * 0.68f), Offset(w * 0.86f, h * 0.84f), stroke.width, StrokeCap.Round)
            }

            LivanaBottomBarIcon.Boards -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.16f, h * 0.22f),
                    size = Size(w * 0.68f, h * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.28f, h * 0.38f), Offset(w * 0.72f, h * 0.38f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.28f, h * 0.54f), Offset(w * 0.58f, h * 0.54f), stroke.width, StrokeCap.Round)
            }

            LivanaBottomBarIcon.Activity -> {
                drawLine(color, Offset(w * 0.2f, h * 0.72f), Offset(w * 0.2f, h * 0.48f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.4f, h * 0.72f), Offset(w * 0.4f, h * 0.32f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.6f, h * 0.72f), Offset(w * 0.6f, h * 0.42f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.8f, h * 0.72f), Offset(w * 0.8f, h * 0.24f), stroke.width, StrokeCap.Round)
            }

            LivanaBottomBarIcon.Profile -> {
                drawCircle(
                    color = color,
                    radius = w * 0.16f,
                    center = Offset(w * 0.5f, h * 0.34f),
                    style = stroke,
                )
                val shoulders = Path().apply {
                    moveTo(w * 0.22f, h * 0.82f)
                    cubicTo(w * 0.28f, h * 0.6f, w * 0.72f, h * 0.6f, w * 0.78f, h * 0.82f)
                }
                drawPath(shoulders, color, style = stroke)
            }
        }
    }
}

@Preview(name = "Livana - Bottom bar", widthDp = 390, heightDp = 140)
@Composable
private fun LivanaBottomBarPreview() {
    LivanaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LivanaColors.Canvas),
        ) {
            LivanaBottomBar(
                selectedDestination = Destination.Home,
                onDestinationSelected = {},
            )
        }
    }
}
