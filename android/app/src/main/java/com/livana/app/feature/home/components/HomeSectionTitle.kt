package com.livana.app.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing

/** Section heading with the jade accent rule (`.sec-title` + `.accent-rule` in the mockups). */
@Composable
internal fun HomeSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(ComponentDimens.BrandMarkSize)
                .height(Spacing.S4)
                .background(
                    Brush.horizontalGradient(listOf(LivanaColors.PrimaryBright, LivanaColors.Primary)),
                    CircleShape,
                ),
        )
        Text(
            text = text,
            color = LivanaColors.Text,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
