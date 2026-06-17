package com.livana.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.Spacing

@Preview(name = "Livana - Empty, error, offline", widthDp = 390, heightDp = 1200)
@Composable
private fun StateTemplatesPreview() {
    LivanaTheme {
        Surface(color = LivanaColors.Canvas) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.ScreenHorizontal),
            ) {
                EmptyState(actionLabel = "Explore causes", onAction = {})
                ErrorState(onRetry = {})
                OfflineState(onRetry = {})
                OfflineBanner(onRetry = {})
            }
        }
    }
}

@Preview(name = "Livana - Loading skeletons", widthDp = 390, heightDp = 1200)
@Composable
private fun LoadingSkeletonsPreview() {
    LivanaTheme {
        Surface(color = LivanaColors.Canvas) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.ScreenHorizontal),
                verticalArrangement = Arrangement.spacedBy(Spacing.S16),
            ) {
                StatCardSkeleton()
                PoolCardSkeleton()
                PoolCardSkeleton()
                androidx.compose.material3.HorizontalDivider(color = LivanaColors.Hairline)
                repeat(3) { ListRowSkeleton() }
                DetailScreenSkeleton()
            }
        }
    }
}
