package com.livana.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.livana.app.core.designsystem.theme.LivanaColors

/**
 * Deterministic gradient avatar derived from an Ethereum address.
 * Used for donor/NGO avatars where the API provides no profile image.
 * The gradient is chosen from a fixed palette based on the character-code sum
 * of the address, so the same address always produces the same avatar.
 *
 * Callers are responsible for applying [Modifier.size] to set the desired dimensions.
 */
@Composable
fun AddressAvatar(
    address: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(address.avatarColors())),
    )
}

private fun String.avatarColors(): List<Color> {
    val palette = listOf(
        listOf(LivanaColors.PrimaryBright, LivanaColors.Primary),
        listOf(LivanaColors.Secondary, LivanaColors.SecondaryInk),
        listOf(LivanaColors.Info, LivanaColors.PendingInk),
        listOf(LivanaColors.Success, LivanaColors.PrimaryPressed),
        listOf(LivanaColors.Gold, LivanaColors.PausedInk),
    )
    val index = fold(0) { acc, char -> acc + char.code }.mod(palette.size)
    return palette[index]
}
