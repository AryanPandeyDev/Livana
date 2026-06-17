@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.livana.app.feature.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.livana.app.BuildConfig
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.LivanaBottomSheet
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.LivanaChip
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.component.LivanaProgress
import com.livana.app.core.designsystem.component.LivanaTextField
import com.livana.app.core.designsystem.component.LivanaTextButton
import com.livana.app.core.designsystem.component.StatusPill
import com.livana.app.core.designsystem.component.StatusPillKind
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.Region
import com.livana.app.core.model.Usdc
import com.livana.app.core.ui.EmptyState
import com.livana.app.core.ui.ErrorState
import com.livana.app.core.ui.OfflineState
import com.livana.app.core.ui.PoolCardSkeleton
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.snapshotFlow

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onOpenPool: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(listState, state) {
        val contentState = state as? ExploreUiState.Content ?: return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastVisibleIndex -> lastVisibleIndex >= contentState.pools.size - PaginationThreshold }
            .distinctUntilChanged()
            .filter { nearEnd -> nearEnd }
            .collect {
                viewModel.loadNextPage()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas)
            .statusBarsPadding(),
    ) {
        ExploreHeader(
            searchQuery = state.searchQuery,
            selectedRegion = state.selectedRegion,
            sortOption = state.sortOption,
            onSearchChange = viewModel::onSearchChange,
            onFilterClick = { showFilterSheet = true },
            onClearRegion = { viewModel.applyFilters(region = null, sortOption = state.sortOption) },
            onSortChipClick = { showFilterSheet = true },
        )
        when (val currentState = state) {
            is ExploreUiState.Loading -> ExploreLoadingList()
            is ExploreUiState.Content -> ExplorePoolList(
                state = currentState,
                listState = listState,
                onOpenPool = onOpenPool,
            )

            is ExploreUiState.Empty -> ExploreStateContainer {
                EmptyState(
                    title = "No causes match",
                    message = "Try a different search or clear filters.",
                    actionLabel = "Clear search",
                    onAction = { viewModel.onSearchChange("") },
                )
            }

            is ExploreUiState.Error -> ExploreStateContainer {
                ErrorState(
                    message = currentState.message ?: "We couldn't load causes. Please try again.",
                    onRetry = viewModel::retry,
                )
            }

            is ExploreUiState.Offline -> ExploreStateContainer {
                OfflineState(onRetry = viewModel::retry)
            }
        }
    }

    if (showFilterSheet) {
        FilterSortSheet(
            selectedRegion = state.selectedRegion,
            selectedSort = state.sortOption,
            onDismiss = { showFilterSheet = false },
            onApply = { region, sortOption ->
                viewModel.applyFilters(region = region, sortOption = sortOption)
                showFilterSheet = false
            },
        )
    }
}

@Composable
private fun ExploreHeader(
    searchQuery: String,
    selectedRegion: Region?,
    sortOption: ExploreSortOption,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onClearRegion: () -> Unit,
    onSortChipClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenHorizontal)
            .padding(top = Spacing.S8),
    ) {
        Text(
            text = "Explore causes",
            color = LivanaColors.Text,
            style = MaterialTheme.typography.headlineLarge,
        )
        Row(
            modifier = Modifier.padding(top = Spacing.S12),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
            verticalAlignment = Alignment.Top,
        ) {
            LivanaTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = "Search causes...",
                leadingContent = { SearchGlyph() },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            IconButtonLivana(
                onClick = onFilterClick,
                contentDescription = "Filter and sort",
            ) {
                FilterGlyph()
            }
        }
        FlowRow(
            modifier = Modifier.padding(top = Spacing.S4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
            verticalArrangement = Arrangement.spacedBy(Spacing.S8),
        ) {
            if (selectedRegion != null) {
                LivanaChip(
                    text = selectedRegion.display,
                    selected = true,
                    onClick = onClearRegion,
                    trailingIcon = { CloseGlyph() },
                )
            }
            LivanaChip(
                text = sortOption.label,
                selected = true,
                onClick = onSortChipClick,
            )
        }
    }
}

@Composable
private fun FilterSortSheet(
    selectedRegion: Region?,
    selectedSort: ExploreSortOption,
    onDismiss: () -> Unit,
    onApply: (Region?, ExploreSortOption) -> Unit,
) {
    var draftRegion by remember(selectedRegion) { mutableStateOf(selectedRegion) }
    var draftSort by remember(selectedSort) { mutableStateOf(selectedSort) }

    LivanaBottomSheet(onDismissRequest = onDismiss) {
        SectionHeading(text = "Filter & sort")
        SheetLabel(
            text = "Region",
            modifier = Modifier.padding(top = Spacing.S16),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.S8),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
            verticalArrangement = Arrangement.spacedBy(Spacing.S8),
        ) {
            Region.entries.forEach { region ->
                LivanaChip(
                    text = region.display,
                    selected = draftRegion == region,
                    onClick = {
                        draftRegion = if (draftRegion == region) null else region
                    },
                )
            }
        }

        SheetLabel(
            text = "Sort by",
            modifier = Modifier.padding(top = Spacing.S24),
        )
        Column(modifier = Modifier.padding(top = Spacing.S8)) {
            ExploreSortOption.entries.forEachIndexed { index, option ->
                SortOptionRow(
                    option = option,
                    selected = draftSort == option,
                    onClick = { draftSort = option },
                )
                if (index != ExploreSortOption.entries.lastIndex) {
                    DividerLine()
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = Spacing.S24),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LivanaTextButton(
                text = "Reset",
                onClick = {
                    draftRegion = null
                    draftSort = ExploreSortOption.Newest
                },
            )
            LivanaPrimaryButton(
                text = "Apply",
                onClick = { onApply(draftRegion, draftSort) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ComponentDimens.BrandMarkSize, Spacing.S4)
                .background(
                    Brush.horizontalGradient(listOf(LivanaColors.PrimaryBright, LivanaColors.Primary)),
                    androidx.compose.foundation.shape.CircleShape,
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
private fun SheetLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = LivanaColors.Text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun SortOptionRow(
    option: ExploreSortOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = Spacing.S12),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioGlyph(selected = selected)
        Text(
            text = option.label,
            color = if (selected) LivanaColors.Text else LivanaColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun RadioGlyph(selected: Boolean) {
    Canvas(modifier = Modifier.size(ComponentDimens.IconSize)) {
        val strokeWidth = Borders.Focus.toPx()
        drawCircle(
            color = if (selected) LivanaColors.Primary else LivanaColors.Border,
            radius = size.minDimension / 2f - strokeWidth / 2f,
            style = Stroke(strokeWidth),
        )
        if (selected) {
            drawCircle(
                color = LivanaColors.Primary,
                radius = size.minDimension * 0.25f,
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Borders.Hairline)
            .background(LivanaColors.Hairline),
    )
}

@Composable
private fun ExplorePoolList(
    state: ExploreUiState.Content,
    listState: LazyListState,
    onOpenPool: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.ScreenHorizontal,
            top = Spacing.S16,
            end = Spacing.ScreenHorizontal,
            bottom = Spacing.S24,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.S16),
    ) {
        items(
            items = state.pools,
            key = { pool -> pool.onChainAddress },
        ) { pool ->
            ExplorePoolCard(
                pool = pool,
                onClick = { onOpenPool(pool.onChainAddress) },
            )
        }
        if (state.isLoadingMore) {
            item(key = "loading-more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.S12),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ComponentDimens.SmallIconSize),
                        color = LivanaColors.Primary,
                        strokeWidth = Borders.Spinner,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplorePoolCard(
    pool: PoolSummary,
    onClick: () -> Unit,
) {
    LivanaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        style = LivanaCardStyle.Media,
    ) {
        Column {
            ExploreCover(pool = pool)
            Column(modifier = Modifier.padding(Spacing.CompactCard)) {
                Text(
                    text = pool.title,
                    color = LivanaColors.Text,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    text = pool.description,
                    modifier = Modifier.padding(top = Spacing.S4),
                    color = LivanaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                LivanaProgress(
                    progress = pool.donationProgress(),
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
                        text = "${pool.totalDonated.formatWhole()} raised - goal ${pool.targetAmount.formatWhole()}",
                        color = LivanaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = pool.donorMeta(),
                        color = LivanaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreCover(pool: PoolSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ComponentDimens.SkeletonPoolImageHeight)
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
                    Brush.verticalGradient(listOf(Color.Transparent, LivanaColors.ScrimBottom)),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.S12),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                kind = StatusPillKind.Region,
                label = pool.region?.display ?: "Global",
            )
            if (pool.isPaused) {
                StatusPill(kind = StatusPillKind.Paused, label = "Paused")
            }
        }
    }
}

@Composable
private fun ExploreLoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.ScreenHorizontal,
            top = Spacing.S16,
            end = Spacing.ScreenHorizontal,
            bottom = Spacing.S24,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.S16),
    ) {
        items(3) {
            PoolCardSkeleton()
        }
    }
}

@Composable
private fun ExploreStateContainer(content: @Composable () -> Unit) {
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
private fun SearchGlyph() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * 0.1f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawCircle(
            color = color,
            radius = size.width * 0.32f,
            center = Offset(size.width * 0.45f, size.height * 0.45f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.68f, size.height * 0.68f),
            end = Offset(size.width * 0.86f, size.height * 0.86f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun FilterGlyph() {
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * 0.1f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawLine(LivanaColors.Primary, Offset(size.width * 0.12f, size.height * 0.25f), Offset(size.width * 0.88f, size.height * 0.25f), stroke.width)
        drawLine(LivanaColors.Primary, Offset(size.width * 0.25f, size.height * 0.5f), Offset(size.width * 0.75f, size.height * 0.5f), stroke.width)
        drawLine(LivanaColors.Primary, Offset(size.width * 0.38f, size.height * 0.75f), Offset(size.width * 0.62f, size.height * 0.75f), stroke.width)
    }
}

@Composable
private fun CloseGlyph() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(ComponentDimens.StatusIconSize)) {
        val strokeWidth = size.minDimension * 0.16f
        drawLine(
            color = color,
            start = Offset(size.width * 0.22f, size.height * 0.22f),
            end = Offset(size.width * 0.78f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.78f, size.height * 0.22f),
            end = Offset(size.width * 0.22f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
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

private fun PoolSummary.donorMeta(): String {
    return if (totalDonated.atomic > BigInteger.ZERO) "Community funded" else "New cause"
}



private const val PaginationThreshold = 3
