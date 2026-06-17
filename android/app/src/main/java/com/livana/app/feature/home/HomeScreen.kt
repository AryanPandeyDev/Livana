package com.livana.app.feature.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.livana.app.BuildConfig
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.LivanaChip
import com.livana.app.core.designsystem.component.LivanaProgress
import com.livana.app.core.designsystem.component.LivanaTextButton
import com.livana.app.core.designsystem.component.StatusPill
import com.livana.app.core.designsystem.component.StatusPillKind
import com.livana.app.core.designsystem.theme.CardShape
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.Elevations
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.MetricStyles
import com.livana.app.core.designsystem.theme.Radii
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.Usdc
import com.livana.app.core.ui.ErrorState
import com.livana.app.core.ui.OfflineState
import com.livana.app.core.ui.PoolCardSkeleton
import com.livana.app.core.ui.SkeletonBlock
import com.livana.app.core.ui.StatCardSkeleton
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenPool: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas)
            .statusBarsPadding(),
    ) {
        HomeTopBar()
        when (val currentState = state) {
            HomeUiState.Loading -> HomeLoadingContent()
            HomeUiState.Offline -> HomeStateContainer {
                OfflineState(onRetry = viewModel::retry)
            }

            is HomeUiState.Error -> HomeStateContainer {
                ErrorState(
                    message = currentState.message ?: "We couldn't load the impact dashboard. Please try again.",
                    onRetry = viewModel::retry,
                )
            }

            is HomeUiState.Content -> HomeContent(
                state = currentState,
                onOpenPool = onOpenPool,
            )
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenHorizontal, vertical = Spacing.S8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark()
            Text(
                text = "Livana",
                color = LivanaColors.Text,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
        }
        LivanaChip(
            text = "Sign in",
            selected = false,
            onClick = {},
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onOpenPool: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.S24),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.S16),
        ) {
            ImpactHeroCard(
                totalDonated = state.totalDonated,
                totalReleased = state.totalReleased,
            )
            StatsTiles(
                activePools = state.activePools,
                verifiedNgos = state.verifiedNgos,
                totalPools = state.totalPools,
            )
        }
        FeaturedCausesSection(
            pools = state.featuredPools,
            onOpenPool = onOpenPool,
        )
    }
}

@Composable
private fun ImpactHeroCard(
    totalDonated: Usdc,
    totalReleased: Usdc,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = Elevations.Jade,
                shape = CardShape,
                ambientColor = LivanaColors.ShadowJade,
                spotColor = LivanaColors.ShadowJade,
            )
            .clip(CardShape)
            .background(
                Brush.linearGradient(
                    listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroB, LivanaColors.GradHeroC),
                ),
            )
            .drawBehind {
                drawCircle(
                    color = LivanaColors.HeroHighlight,
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.92f, -size.height * 0.12f),
                )
                drawCircle(
                    color = LivanaColors.OnPrimary.copy(alpha = 0.13f),
                    radius = size.width * 0.18f,
                    center = Offset(size.width * 1.03f, -size.height * 0.1f),
                )
            }
            .padding(Spacing.Card),
    ) {
        Column {
            Text(
                text = "Total donated on Livana",
                color = LivanaColors.OnPrimary.copy(alpha = 0.82f),
                style = ComponentTextStyles.Pill.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = totalDonated.formatWhole(),
                modifier = Modifier.padding(top = Spacing.S8),
                color = LivanaColors.OnPrimary,
                style = MetricStyles.Large,
            )
            Row(
                modifier = Modifier.padding(top = Spacing.S12),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${totalReleased.formatWhole()} released to causes",
                    color = LivanaColors.OnPrimary.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelLarge,
                )
                VerifiablePill()
            }
        }
    }
}

@Composable
private fun VerifiablePill() {
    Row(
        modifier = Modifier
            .height(ComponentDimens.PillHeight)
            .background(LivanaColors.OnPrimary.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = ComponentDimens.PillHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(ComponentDimens.PillContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(ComponentDimens.StatusIconSize)) {
            val stroke = Stroke(
                width = size.minDimension * 0.16f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            drawLine(
                color = LivanaColors.OnPrimary,
                start = Offset(size.width * 0.18f, size.height * 0.52f),
                end = Offset(size.width * 0.42f, size.height * 0.76f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = LivanaColors.OnPrimary,
                start = Offset(size.width * 0.42f, size.height * 0.76f),
                end = Offset(size.width * 0.84f, size.height * 0.24f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = "verifiable",
            color = LivanaColors.OnPrimary,
            style = ComponentTextStyles.Pill,
        )
    }
}

@Composable
private fun StatsTiles(
    activePools: Long,
    verifiedNgos: Long,
    totalPools: Long,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
    ) {
        StatTile(
            value = activePools.toString(),
            label = "Active pools",
            tint = IconChipTint.Jade,
            icon = HomeTileIcon.Pools,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = verifiedNgos.toString(),
            label = "Verified NGOs",
            tint = IconChipTint.Coral,
            icon = HomeTileIcon.Verified,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = totalPools.toString(),
            label = "Total pools",
            tint = IconChipTint.Info,
            icon = HomeTileIcon.Clock,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    tint: IconChipTint,
    icon: HomeTileIcon,
    modifier: Modifier = Modifier,
) {
    LivanaCard(
        modifier = modifier,
        style = LivanaCardStyle.Flat,
        contentPadding = PaddingValues(Spacing.S16),
    ) {
        Column {
            IconChip(
                tint = tint,
                modifier = Modifier.size(ComponentDimens.IconButtonVisualSize),
            ) {
                HomeTileGlyph(icon = icon)
            }
            Text(
                text = value,
                modifier = Modifier.padding(top = Spacing.S12),
                color = LivanaColors.Text,
                style = MetricStyles.Display,
            )
            Text(
                text = label,
                modifier = Modifier.padding(top = Spacing.S4),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun FeaturedCausesSection(
    pools: List<PoolSummary>,
    onOpenPool: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.ScreenHorizontal, top = Spacing.S24, end = Spacing.ScreenHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(text = "Featured causes")
        LivanaTextButton(text = "See all", onClick = {})
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

@Composable
private fun SectionTitle(text: String) {
    Row(
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

@Composable
private fun HomeLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.ScreenHorizontal, vertical = Spacing.S8),
        verticalArrangement = Arrangement.spacedBy(Spacing.S16),
    ) {
        StatCardSkeleton()
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S12)) {
            repeat(3) {
                LivanaCard(
                    modifier = Modifier.weight(1f),
                    style = LivanaCardStyle.Flat,
                    contentPadding = PaddingValues(Spacing.S16),
                ) {
                    Column {
                        SkeletonBlock(Modifier.size(ComponentDimens.IconButtonVisualSize), shape = androidx.compose.foundation.shape.RoundedCornerShape(Radii.Input))
                        SkeletonBlock(
                            modifier = Modifier
                                .padding(top = Spacing.S12)
                                .fillMaxWidth(0.62f)
                                .height(ComponentDimens.SkeletonMetricHeight),
                        )
                        SkeletonBlock(
                            modifier = Modifier
                                .padding(top = Spacing.S8)
                                .fillMaxWidth()
                                .height(ComponentDimens.SkeletonCaptionHeight),
                        )
                    }
                }
            }
        }
        SkeletonBlock(
            modifier = Modifier
                .padding(top = Spacing.S8)
                .fillMaxWidth(0.54f)
                .height(ComponentDimens.SkeletonTitleHeight),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S16)) {
            repeat(2) {
                PoolCardSkeleton(modifier = Modifier.width(ComponentDimens.MinimumTouchTarget * 5f))
            }
        }
    }
}

@Composable
private fun HomeStateContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.ScreenHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun HomeTileGlyph(icon: HomeTileIcon) {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * 0.1f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val w = size.width
        val h = size.height
        when (icon) {
            HomeTileIcon.Pools -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.24f),
                    size = Size(w * 0.76f, h * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.12f, h * 0.43f), Offset(w * 0.88f, h * 0.43f), stroke.width)
            }

            HomeTileIcon.Verified -> {
                val shield = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.82f, h * 0.26f)
                    lineTo(w * 0.82f, h * 0.5f)
                    cubicTo(w * 0.82f, h * 0.72f, w * 0.66f, h * 0.84f, w * 0.5f, h * 0.9f)
                    cubicTo(w * 0.34f, h * 0.84f, w * 0.18f, h * 0.72f, w * 0.18f, h * 0.5f)
                    lineTo(w * 0.18f, h * 0.26f)
                    close()
                }
                drawPath(shield, color, style = stroke)
                drawLine(color, Offset(w * 0.36f, h * 0.53f), Offset(w * 0.47f, h * 0.64f), stroke.width)
                drawLine(color, Offset(w * 0.47f, h * 0.64f), Offset(w * 0.66f, h * 0.4f), stroke.width)
            }

            HomeTileIcon.Clock -> {
                drawCircle(color, radius = w * 0.38f, style = stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.5f, h * 0.52f), stroke.width)
                drawLine(color, Offset(w * 0.5f, h * 0.52f), Offset(w * 0.66f, h * 0.66f), stroke.width)
            }
        }
    }
}

private enum class HomeTileIcon {
    Pools,
    Verified,
    Clock,
}

private fun PoolSummary.donationProgress(): Float {
    if (targetAmount.atomic.signum() <= 0) return 0f
    return totalDonated.atomic.toBigDecimal()
        .divide(targetAmount.atomic.toBigDecimal(), 4, RoundingMode.DOWN)
        .coerceAtMost(BigDecimal.ONE)
        .toFloat()
}

