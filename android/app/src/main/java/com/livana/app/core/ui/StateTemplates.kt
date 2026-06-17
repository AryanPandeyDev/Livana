package com.livana.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.livana.app.core.designsystem.component.LivanaGlyph
import com.livana.app.core.designsystem.component.LivanaGlyphKind
import com.livana.app.core.designsystem.component.LivanaOutlineButton
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.component.LivanaTonalButton
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Radii
import com.livana.app.core.designsystem.theme.Spacing

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String = "Nothing here yet",
    message: String = "When there's activity, it'll show up here.",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateTemplate(
        title = title,
        message = message,
        modifier = modifier,
        containerColor = LivanaColors.PrimaryContainer,
        contentColor = LivanaColors.Primary,
        glyph = LivanaGlyphKind.Empty,
        action = if (actionLabel != null && onAction != null) {
            {
                LivanaTonalButton(text = actionLabel, onClick = onAction)
            }
        } else {
            null
        },
    )
}

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    message: String = "We couldn't load this. Please try again.",
    actionLabel: String = "Try again",
    onRetry: () -> Unit,
) {
    StateTemplate(
        title = title,
        message = message,
        modifier = modifier,
        containerColor = LivanaColors.SecondaryContainer,
        contentColor = LivanaColors.SecondaryInk,
        glyph = LivanaGlyphKind.Alert,
        action = {
            LivanaPrimaryButton(text = actionLabel, onClick = onRetry)
        },
    )
}

@Composable
fun OfflineState(
    modifier: Modifier = Modifier,
    title: String = "You're offline",
    message: String = "Check your connection and retry.",
    actionLabel: String = "Retry",
    onRetry: () -> Unit,
) {
    StateTemplate(
        title = title,
        message = message,
        modifier = modifier,
        containerColor = LivanaColors.SurfaceAlt,
        contentColor = LivanaColors.TextSecondary,
        glyph = LivanaGlyphKind.Offline,
        action = {
            LivanaOutlineButton(text = actionLabel, onClick = onRetry)
        },
    )
}

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
    message: String = "You're offline. Showing saved content.",
    onRetry: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LivanaColors.SurfaceAlt, RoundedCornerShape(Radii.Input))
            .padding(Spacing.S12),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LivanaGlyph(LivanaGlyphKind.Offline, color = LivanaColors.TextSecondary)
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = LivanaColors.TextSecondary,
            style = ComponentTextStyles.StateBody,
        )
        if (onRetry != null) {
            LivanaTonalButton(text = "Retry", onClick = onRetry)
        }
    }
}

@Composable
private fun StateTemplate(
    title: String,
    message: String,
    containerColor: Color,
    contentColor: Color,
    glyph: LivanaGlyphKind,
    modifier: Modifier,
    action: (@Composable () -> Unit)?,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.S32),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(ComponentDimens.StateIllustrationSize)
                .background(containerColor, RoundedCornerShape(Radii.StateIllustration)),
            contentAlignment = Alignment.Center,
        ) {
            LivanaGlyph(
                kind = glyph,
                color = contentColor,
                size = ComponentDimens.StateIllustrationIconSize,
            )
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = Spacing.S16),
            color = LivanaColors.Text,
            style = ComponentTextStyles.StateTitle,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = Spacing.S8),
            color = LivanaColors.TextSecondary,
            style = ComponentTextStyles.StateBody,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Box(modifier = Modifier.padding(top = Spacing.S16)) {
                action()
            }
        }
    }
}
