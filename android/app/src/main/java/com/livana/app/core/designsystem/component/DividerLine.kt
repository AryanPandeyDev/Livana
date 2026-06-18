package com.livana.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.LivanaColors

@Composable
fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Borders.Hairline)
            .background(LivanaColors.Hairline),
    )
}
