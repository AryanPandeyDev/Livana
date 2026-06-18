package com.livana.app.feature.pooldetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.livana.app.core.designsystem.component.HeartIcon
import com.livana.app.core.designsystem.component.LivanaButtonState
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.component.LivanaTonalButton
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing

@Composable
fun DonateDock(
    isPaused: Boolean,
    onDonate: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LivanaColors.Surface)
            .navigationBarsPadding()
            .padding(
                start = Spacing.ScreenHorizontal,
                end = Spacing.ScreenHorizontal,
                top = Spacing.S16,
                bottom = Spacing.S20,
            ),
    ) {
        if (isPaused) {
            LivanaTonalButton(
                text = "Donations paused",
                onClick = {},
                state = LivanaButtonState.Disabled,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LivanaPrimaryButton(
                text = "Donate",
                onClick = onDonate,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { HeartIcon(tint = LivanaColors.OnPrimary) },
            )
        }
    }
}
