package com.livana.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.InputShape
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing

@Composable
fun LivanaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    multiline: Boolean = false,
    suffix: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val supportText = errorText ?: helperText.orEmpty()
    val supportColor = if (errorText != null) LivanaColors.Error else LivanaColors.TextSecondary
    val innerBorderColor = if (errorText != null) LivanaColors.Error else Color.Transparent
    val containerModifier = Modifier
        .fillMaxWidth()
        .then(
            if (multiline) {
                Modifier.heightIn(min = ComponentDimens.InputAreaMinimumHeight)
            } else {
                Modifier.height(ComponentDimens.InputHeight)
            },
        )
        .background(LivanaColors.SurfaceAlt, InputShape)
        .border(Borders.Control, innerBorderColor, InputShape)
        .padding(
            horizontal = ComponentDimens.InputHorizontalPadding,
            vertical = if (multiline) ComponentDimens.InputAreaVerticalPadding else Dp.Hairline,
        )

    Column(modifier = modifier.alpha(if (enabled) 1f else 0.5f)) {
        if (label != null) {
            Text(
                text = label,
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = Spacing.S8),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = Borders.Focus,
                    color = if (focused && enabled) LivanaColors.Focus else Color.Transparent,
                    shape = InputShape,
                )
                .padding(Borders.Focus + Borders.FocusOffset)
                .then(if (!enabled) Modifier.semantics { disabled() } else Modifier),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = containerModifier,
                enabled = enabled,
                readOnly = readOnly,
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                maxLines = if (multiline) 5 else 1,
                textStyle = ComponentTextStyles.Field.copy(color = LivanaColors.Text),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                cursorBrush = Brush.verticalGradient(listOf(LivanaColors.Primary, LivanaColors.Primary)),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
                        verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
                    ) {
                        if (leadingContent != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides LivanaColors.TextSecondary,
                                content = leadingContent,
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    text = placeholder,
                                    color = LivanaColors.TextMuted,
                                    style = ComponentTextStyles.Field,
                                )
                            }
                            innerTextField()
                        }
                        if (suffix != null) {
                            Text(
                                text = suffix,
                                color = LivanaColors.TextSecondary,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                ),
                            )
                        }
                        if (trailingContent != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides LivanaColors.TextMuted,
                                content = trailingContent,
                            )
                        }
                    }
                },
            )
        }
        Text(
            text = supportText,
            color = supportColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .defaultMinSize(minHeight = ComponentDimens.HelperMinimumHeight)
                .padding(top = Spacing.S4),
        )
    }
}
