package com.livana.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.Elevations
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.MotionTokens
import com.livana.app.core.designsystem.theme.PillShape
import com.livana.app.core.designsystem.theme.reducedMotionEnabled

enum class LivanaButtonState {
    Enabled,
    Loading,
    Success,
    Error,
    Disabled,
}

private enum class LivanaButtonStyle {
    Primary,
    Coral,
    Tonal,
    Outline,
    Text,
}

@Composable
fun LivanaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: LivanaButtonState = LivanaButtonState.Enabled,
    leadingIcon: (@Composable () -> Unit)? = null,
) = LivanaButton(text, onClick, modifier, state, leadingIcon, LivanaButtonStyle.Primary)

@Composable
fun LivanaCoralButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: LivanaButtonState = LivanaButtonState.Enabled,
    leadingIcon: (@Composable () -> Unit)? = null,
) = LivanaButton(text, onClick, modifier, state, leadingIcon, LivanaButtonStyle.Coral)

@Composable
fun LivanaTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: LivanaButtonState = LivanaButtonState.Enabled,
    leadingIcon: (@Composable () -> Unit)? = null,
) = LivanaButton(text, onClick, modifier, state, leadingIcon, LivanaButtonStyle.Tonal)

@Composable
fun LivanaOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: LivanaButtonState = LivanaButtonState.Enabled,
    leadingIcon: (@Composable () -> Unit)? = null,
) = LivanaButton(text, onClick, modifier, state, leadingIcon, LivanaButtonStyle.Outline)

@Composable
fun LivanaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: LivanaButtonState = LivanaButtonState.Enabled,
    leadingIcon: (@Composable () -> Unit)? = null,
) = LivanaButton(text, onClick, modifier, state, leadingIcon, LivanaButtonStyle.Text)

@Composable
private fun LivanaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    state: LivanaButtonState,
    leadingIcon: (@Composable () -> Unit)?,
    style: LivanaButtonStyle,
) {
    val enabled = state != LivanaButtonState.Disabled && state != LivanaButtonState.Loading
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val reducedMotion = reducedMotionEnabled()
    val targetScale = if (pressed && enabled && style != LivanaButtonStyle.Text) MotionTokens.PressedScale else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(if (reducedMotion) 0 else MotionTokens.FastMillis),
        label = "livana-button-scale",
    )
    val visual = buttonVisuals(style, state, pressed, hovered)
    val shape = PillShape
    val focusColor = if (focused && enabled) LivanaColors.Focus else Color.Transparent
    val shadowModifier = if (visual.elevation == null) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = visual.elevation,
            shape = shape,
            clip = false,
            ambientColor = visual.shadowColor,
            spotColor = visual.shadowColor,
        )
    }

    Box(
        modifier = modifier
            .border(Borders.Focus, focusColor, RoundedCornerShape(percent = 50))
            .padding(Borders.Focus + Borders.FocusOffset)
            .then(shadowModifier)
            .scale(scale)
            .height(if (style == LivanaButtonStyle.Text) ComponentDimens.CompactButtonHeight else ComponentDimens.ButtonHeight)
            .defaultMinSize(minWidth = ComponentDimens.MinimumTouchTarget)
            .background(visual.container, shape)
            .then(
                if (style == LivanaButtonStyle.Outline) {
                    Modifier.border(Borders.Emphasis, LivanaColors.Primary, shape)
                } else {
                    Modifier
                },
            )
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
                if (!enabled) disabled()
            }
            .alpha(if (state == LivanaButtonState.Disabled) 0.5f else 1f)
            .padding(horizontal = ComponentDimens.ButtonHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ButtonContentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (state) {
                LivanaButtonState.Loading -> LivanaSpinner(color = visual.content, trackColor = visual.content.copy(alpha = 0.3f))
                LivanaButtonState.Success -> LivanaGlyph(LivanaGlyphKind.Check, color = visual.content)
                LivanaButtonState.Error -> LivanaGlyph(LivanaGlyphKind.Alert, color = visual.content)
                else -> leadingIcon?.invoke()
            }
            Text(
                text = text,
                color = visual.content,
                style = ComponentTextStyles.Button,
            )
        }
    }
}

private data class ButtonVisuals(
    val container: Color,
    val content: Color,
    val elevation: androidx.compose.ui.unit.Dp? = null,
    val shadowColor: Color = Color.Transparent,
)

private fun buttonVisuals(
    style: LivanaButtonStyle,
    state: LivanaButtonState,
    pressed: Boolean,
    hovered: Boolean,
): ButtonVisuals {
    if (state == LivanaButtonState.Success) {
        return ButtonVisuals(LivanaColors.Success, LivanaColors.OnPrimary)
    }
    if (state == LivanaButtonState.Error) {
        return ButtonVisuals(LivanaColors.Error, LivanaColors.OnPrimary)
    }
    return when (style) {
        LivanaButtonStyle.Primary -> ButtonVisuals(
            container = if (pressed || hovered) LivanaColors.PrimaryPressed else LivanaColors.Primary,
            content = LivanaColors.OnPrimary,
            elevation = if (state == LivanaButtonState.Disabled) null else Elevations.Jade,
            shadowColor = LivanaColors.ShadowJade,
        )
        LivanaButtonStyle.Coral -> ButtonVisuals(
            container = if (pressed || hovered) LivanaColors.SecondaryPressed else LivanaColors.Secondary,
            content = LivanaColors.OnPrimary,
            elevation = if (state == LivanaButtonState.Disabled) null else Elevations.Coral,
            shadowColor = LivanaColors.ShadowCoral,
        )
        LivanaButtonStyle.Tonal -> ButtonVisuals(
            container = if (pressed || hovered) LivanaColors.TonalHovered else LivanaColors.PrimaryContainer,
            content = LivanaColors.Primary,
        )
        LivanaButtonStyle.Outline -> ButtonVisuals(
            container = if (pressed || hovered) LivanaColors.PrimaryContainer else Color.Transparent,
            content = LivanaColors.Primary,
        )
        LivanaButtonStyle.Text -> ButtonVisuals(
            container = Color.Transparent,
            content = if (pressed || hovered) LivanaColors.PrimaryPressed else LivanaColors.Primary,
        )
    }
}
