package com.livana.app.feature.pooldetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaProgress
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.MetricStyles
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolDetail
import com.livana.app.feature.pooldetail.ProgressScale
import java.math.RoundingMode
import java.math.BigDecimal

@Composable
fun PoolProgressCard(pool: PoolDetail) {
    LivanaCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
            ) {
                Text(
                    text = pool.totalDonated.formatWhole(),
                    color = LivanaColors.Text,
                    style = MetricStyles.Display,
                )
                Text(
                    text = "of ${pool.targetAmount.formatWhole()} goal",
                    modifier = Modifier.padding(bottom = Spacing.S4),
                    color = LivanaColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            LivanaProgress(
                progress = donationProgress(pool),
                modifier = Modifier.padding(top = Spacing.S16),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.S8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${pool.donationCount} donors",
                    color = LivanaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "${pool.totalReleased.formatWhole()} released",
                    color = LivanaColors.Primary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

private fun donationProgress(pool: PoolDetail): Float {
    if (pool.targetAmount.atomic.signum() <= 0) return 0f
    return pool.totalDonated.atomic.toBigDecimal()
        .divide(pool.targetAmount.atomic.toBigDecimal(), ProgressScale, RoundingMode.DOWN)
        .coerceAtMost(BigDecimal.ONE)
        .toFloat()
}
