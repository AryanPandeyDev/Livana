package com.livana.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Single light scheme. NO dark theme — do not add one.
private val LivanaLightScheme = lightColorScheme(
    primary = LivanaColors.Primary,
    onPrimary = LivanaColors.OnPrimary,
    primaryContainer = LivanaColors.PrimaryContainer,
    onPrimaryContainer = LivanaColors.Primary,
    secondary = LivanaColors.Secondary,
    onSecondary = LivanaColors.OnPrimary,
    secondaryContainer = LivanaColors.SecondaryContainer,
    onSecondaryContainer = LivanaColors.SecondaryInk,
    background = LivanaColors.Canvas,
    onBackground = LivanaColors.Text,
    surface = LivanaColors.Surface,
    onSurface = LivanaColors.Text,
    surfaceVariant = LivanaColors.SurfaceAlt,
    onSurfaceVariant = LivanaColors.TextSecondary,
    outline = LivanaColors.Border,
    outlineVariant = LivanaColors.Hairline,
    error = LivanaColors.Error,
    onError = LivanaColors.OnPrimary,
)

// Tokens M3 doesn't model (muted text, gold, status colors, etc.) ride along here.
data class LivanaExtendedColors(
    val textMuted: Color = LivanaColors.TextMuted,
    val gold: Color = LivanaColors.Gold,
    val goldContainer: Color = LivanaColors.GoldContainer,
    val success: Color = LivanaColors.Success,
    val warning: Color = LivanaColors.Warning,
    val info: Color = LivanaColors.Info,
    val hairline: Color = LivanaColors.Hairline,
    val surface2: Color = LivanaColors.Surface2,
)

val LocalLivanaColors = staticCompositionLocalOf { LivanaExtendedColors() }

@Composable
fun LivanaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLivanaColors provides LivanaExtendedColors()) {
        MaterialTheme(
            colorScheme = LivanaLightScheme,
            typography = LivanaTypography,
            shapes = LivanaShapes,
            content = content,
        )
    }
}
