package com.livana.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.Elevations
import com.livana.app.core.designsystem.theme.IconChipShape
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.PillShape
import com.livana.app.core.designsystem.theme.Radii

@Composable
fun LivanaChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val container = when {
        selected -> LivanaColors.PrimaryContainer
        pressed || hovered -> LivanaColors.NeutralHovered
        else -> LivanaColors.SurfaceAlt
    }
    val content = if (selected) LivanaColors.Primary else LivanaColors.TextSecondary

    Box(
        modifier = modifier
            .border(
                width = Borders.Focus,
                color = if (focused && enabled) LivanaColors.Focus else Color.Transparent,
                shape = PillShape,
            )
            .padding(Borders.Focus + Borders.FocusOffset)
            .height(ComponentDimens.ChipHeight)
            .defaultMinSize(minWidth = ComponentDimens.MinimumTouchTarget)
            .background(container, PillShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .hoverable(interactionSource, enabled)
            .focusable(enabled, interactionSource)
            .semantics {
                role = Role.Button
                this.selected = selected
                if (!enabled) disabled()
            }
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = ComponentDimens.ChipHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ChipContentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            Text(text = text, color = content, style = ComponentTextStyles.Chip)
            trailingIcon?.invoke()
        }
    }
}

enum class StatusPillKind {
    Verified,
    Featured,
    Paused,
    Released,
    Pending,
    Region,
}

@Composable
fun StatusPill(
    kind: StatusPillKind,
    modifier: Modifier = Modifier,
    label: String = kind.defaultLabel(),
) {
    val colors = kind.colors()
    Row(
        modifier = modifier
            .height(ComponentDimens.PillHeight)
            .background(colors.first, PillShape)
            .padding(horizontal = ComponentDimens.PillHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.PillContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LivanaGlyph(
            kind = kind.glyph(),
            color = colors.second,
            size = ComponentDimens.StatusIconSize,
        )
        Text(
            text = label,
            color = colors.second,
            style = ComponentTextStyles.Pill,
        )
    }
}

enum class IconChipTint {
    Jade,
    Coral,
    Gold,
    Info,
}

@Composable
fun IconChip(
    tint: IconChipTint,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = tint.colors()
    Box(
        modifier = modifier
            .size(ComponentDimens.IconChipSize)
            .background(colors.first, IconChipShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides colors.second,
            content = content,
        )
    }
}

enum class LivanaIconButtonStyle {
    Standard,
    Glass,
}

@Composable
fun IconButtonLivana(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: LivanaIconButtonStyle = LivanaIconButtonStyle.Standard,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val container = if (style == LivanaIconButtonStyle.Glass) LivanaColors.GlassSurface else LivanaColors.Surface
    val shadowColor = if (style == LivanaIconButtonStyle.Glass) LivanaColors.ShadowGlass else LivanaColors.ShadowSmall

    Box(
        modifier = modifier
            .size(ComponentDimens.MinimumTouchTarget)
            .border(
                Borders.Focus,
                if (focused && enabled) LivanaColors.Focus else Color.Transparent,
                androidx.compose.foundation.shape.RoundedCornerShape(Radii.Button),
            )
            .padding(Borders.FocusOffset)
            .shadow(
                Elevations.Small,
                androidx.compose.foundation.shape.RoundedCornerShape(Radii.Input),
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .size(ComponentDimens.IconButtonVisualSize)
            .background(container, androidx.compose.foundation.shape.RoundedCornerShape(Radii.Input))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(enabled, interactionSource)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                if (!enabled) disabled()
            }
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun StatusPillKind.defaultLabel(): String = when (this) {
    StatusPillKind.Verified -> "Verified"
    StatusPillKind.Featured -> "Featured"
    StatusPillKind.Paused -> "Paused"
    StatusPillKind.Released -> "Released"
    StatusPillKind.Pending -> "Pending"
    StatusPillKind.Region -> "Region"
}

private fun StatusPillKind.glyph(): LivanaGlyphKind = when (this) {
    StatusPillKind.Verified -> LivanaGlyphKind.Check
    StatusPillKind.Featured -> LivanaGlyphKind.Star
    StatusPillKind.Paused -> LivanaGlyphKind.Pause
    StatusPillKind.Released -> LivanaGlyphKind.Unlock
    StatusPillKind.Pending -> LivanaGlyphKind.Clock
    StatusPillKind.Region -> LivanaGlyphKind.Pin
}

private fun StatusPillKind.colors(): Pair<Color, Color> = when (this) {
    StatusPillKind.Verified -> LivanaColors.PrimaryContainer to LivanaColors.Primary
    StatusPillKind.Featured -> LivanaColors.SecondaryContainer to LivanaColors.SecondaryInk
    StatusPillKind.Paused -> LivanaColors.GoldContainer to LivanaColors.PausedInk
    StatusPillKind.Released -> LivanaColors.ReleasedBg to LivanaColors.ReleasedInk
    StatusPillKind.Pending -> LivanaColors.PendingBg to LivanaColors.PendingInk
    StatusPillKind.Region -> LivanaColors.RegionSurface to LivanaColors.Text
}

private fun IconChipTint.colors(): Pair<Color, Color> = when (this) {
    IconChipTint.Jade -> LivanaColors.PrimaryContainer to LivanaColors.Primary
    IconChipTint.Coral -> LivanaColors.SecondaryContainer to LivanaColors.SecondaryInk
    IconChipTint.Gold -> LivanaColors.GoldContainer to LivanaColors.PausedInk
    IconChipTint.Info -> LivanaColors.PendingBg to LivanaColors.Info
}
