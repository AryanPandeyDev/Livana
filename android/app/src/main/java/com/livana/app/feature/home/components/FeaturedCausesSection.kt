package com.livana.app.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import coil.compose.AsyncImage
import com.livana.app.BuildConfig
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.LivanaProgress
import com.livana.app.core.designsystem.component.LivanaTextButton
import com.livana.app.core.designsystem.component.StatusPill
import com.livana.app.core.designsystem.component.StatusPillKind
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolSummary
import java.math.BigDecimal
import java.math.RoundingMode

/** Horizontally scrolling featured-pools carousel with section header (02-home.html). */
@Composable
internal fun FeaturedCausesSection(
    pools: List<PoolSummary>,
    onOpenPool: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSeeAll: () -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.ScreenHorizontal,
                    top = Spacing.S24,
                    end = Spacing.ScreenHorizontal,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeSectionTitle(text = "Featured causes")
            LivanaTextButton(text = "See all", onClick = onSeeAll)
        }

        if (pools.isEmpty()) {
            LivanaCard(
                modifier = Modifier
                    .padding(horizontal = Spacing.ScreenHorizontal, vertical = Spacing.S12)
                    .fillMaxWidth(),
                style = LivanaCardStyle.Flat,
            ) {
                Text(
                    text = "No featured causes yet.",
                    color = LivanaColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.padding(top = Spacing.S12),
                contentPadding = PaddingValues(horizontal = Spacing.ScreenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S16),
            ) {
                items(
                    items = pools,
                    key = { pool -> pool.onChainAddress },
                ) { pool ->
                    FeaturedPoolCard(
                        pool = pool,
                        onClick = { onOpenPool(pool.onChainAddress) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedPoolCard(
    pool: PoolSummary,
    onClick: () -> Unit,
) {
    LivanaCard(
        modifier = Modifier
            .width(ComponentDimens.MinimumTouchTarget * 5f)
            .clickable(role = Role.Button, onClick = onClick),
        style = LivanaCardStyle.Media,
    ) {
        Column {
            PoolCover(pool = pool)
            Column(
                modifier = Modifier.padding(Spacing.CompactCard),
            ) {
                Text(
                    text = pool.title,
                    color = LivanaColors.Text,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                )
                LivanaProgress(
                    progress = pool.donationProgress(),
                    modifier = Modifier.padding(top = Spacing.S12),
                )
                Row(
                    modifier = Modifier.padding(top = Spacing.S8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pool.totalDonated.formatWhole(),
                        color = LivanaColors.Text,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = " of ${pool.targetAmount.formatWhole()}",
                        color = LivanaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PoolCover(pool: PoolSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentDimens.SkeletonPoolImageHeight - Spacing.S20)
            .background(
                Brush.linearGradient(listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroB)),
            ),
    ) {
        val coverUrl = pool.coverImageCid?.let { cid ->
            BuildConfig.IPFS_GATEWAY.trimEnd('/') + "/" + cid
        }
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = pool.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            BrandMark(
                modifier = Modifier.align(Alignment.Center),
                size = ComponentDimens.IconChipSize,
                petalColor = LivanaColors.OnPrimary.copy(alpha = 0.8f),
                centerColor = LivanaColors.SecondaryContainer,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, LivanaColors.ScrimBottom),
                    ),
                ),
        )
        StatusPill(
            kind = if (pool.isPaused) StatusPillKind.Paused else StatusPillKind.Region,
            label = if (pool.isPaused) "Paused" else pool.region?.display ?: "Global",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.S12),
        )
    }
}

private fun PoolSummary.donationProgress(): Float {
    if (targetAmount.atomic.signum() <= 0) return 0f
    return totalDonated.atomic.toBigDecimal()
        .divide(targetAmount.atomic.toBigDecimal(), 4, RoundingMode.DOWN)
        .coerceAtMost(BigDecimal.ONE)
        .toFloat()
}
