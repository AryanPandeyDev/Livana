package com.livana.app.feature.reputation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livana.app.core.common.toShortDate
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.DividerLine
import com.livana.app.core.designsystem.component.AddressText
import com.livana.app.core.designsystem.component.BackChevronIcon
import com.livana.app.core.designsystem.component.CheckIcon
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.MedalIcon
import com.livana.app.core.designsystem.component.ShieldCheckIcon
import com.livana.app.core.designsystem.component.truncateAddress
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.MetricStyles
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.NgoReputation
import com.livana.app.core.model.SbtMint
import com.livana.app.core.model.Usdc
import com.livana.app.core.ui.EmptyState
import com.livana.app.core.ui.ErrorState
import com.livana.app.core.ui.ListRowSkeleton
import com.livana.app.core.ui.OfflineState
import com.livana.app.core.ui.SkeletonBlock
import com.livana.app.core.ui.StatCardSkeleton
import java.math.BigInteger
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@Composable
fun NgoProfileScreen(
    viewModel: NgoProfileViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NgoProfileScreen(
        state = state,
        address = viewModel.address,
        onBack = onBack,
        onLoadMore = viewModel::loadNextPage,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun NgoProfileScreen(
    state: NgoProfileUiState,
    address: String = "",
    onBack: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas)
            .statusBarsPadding(),
    ) {
        ProfileAppBar(onBack = onBack)
        when (state) {
            is NgoProfileUiState.Loading -> ProfileLoadingSkeleton()

            is NgoProfileUiState.Content -> ProfileContent(
                reputation = state.reputation,
                history = state.history,
                isLoadingMore = state.isLoadingMore,
                endReached = state.endReached,
                onLoadMore = onLoadMore,
            )

            is NgoProfileUiState.Error -> ProfileStateContainer {
                ErrorState(
                    message = state.message ?: "We couldn't load this profile. Please try again.",
                    onRetry = onRetry,
                )
            }

            is NgoProfileUiState.Offline -> ProfileStateContainer {
                OfflineState(onRetry = onRetry)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// App bar
// ---------------------------------------------------------------------------

@Composable
private fun ProfileAppBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.S8, vertical = Spacing.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonLivana(
            onClick = onBack,
            contentDescription = "Back",
        ) {
            BackChevronIcon()
        }
        Text(
            text = "NGO profile",
            modifier = Modifier.weight(1f),
            color = LivanaColors.Text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        // Balance spacer for the leading icon.
        Spacer(modifier = Modifier.width(ComponentDimens.IconChipSize))
    }
}

// ---------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------

@Composable
private fun ProfileContent(
    reputation: NgoReputation,
    history: List<SbtMint>,
    isLoadingMore: Boolean,
    endReached: Boolean,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, history.size, endReached) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastIndex -> lastIndex >= (history.size + HeaderItemCount) - PaginationThreshold }
            .distinctUntilChanged()
            .filter { nearEnd -> nearEnd }
            .collect { onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.ScreenHorizontal,
            top = Spacing.S4,
            end = Spacing.ScreenHorizontal,
            bottom = Spacing.S24,
        ),
    ) {
        // Header card
        item(key = "header") {
            HeaderCard(reputation = reputation)
        }

        // Stat tiles
        item(key = "stats") {
            StatTiles(
                reputation = reputation,
                modifier = Modifier.padding(top = Spacing.S12),
            )
        }

        // Section title
        item(key = "history-title") {
            SectionTitle(
                text = "Impact history",
                modifier = Modifier.padding(top = Spacing.S24, bottom = Spacing.S12),
            )
        }

        // History card (all rows grouped)
        if (history.isEmpty() && !isLoadingMore) {
            item(key = "history-empty") {
                LivanaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No verified releases yet",
                        message = "SBT mints will appear here after proofs are verified and released.",
                    )
                }
            }
        } else {
            item(key = "history-card") {
                LivanaCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
                ) {
                    Column {
                        history.forEachIndexed { index, sbt ->
                            SbtHistoryRow(sbt = sbt)
                            if (index < history.lastIndex) {
                                DividerLine()
                            }
                        }
                    }
                }
            }
        }

        // Loading more
        if (isLoadingMore) {
            item(key = "loading-more") {
                LoadingMoreIndicator()
            }
        }

        // SBT caption footnote
        item(key = "sbt-caption") {
            SbtCaptionFootnote(
                modifier = Modifier.padding(top = Spacing.S16),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Header card
// ---------------------------------------------------------------------------

@Composable
private fun HeaderCard(reputation: NgoReputation) {
    LivanaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AddressAvatar(
                address = reputation.ngoAddress,
                modifier = Modifier.size(AvatarSize),
            )
            Row(
                modifier = Modifier.padding(top = Spacing.S12),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val orgName = reputation.orgName
                if (orgName != null) {
                    Text(
                        text = orgName,
                        color = LivanaColors.Text,
                        style = MaterialTheme.typography.titleLarge,
                    )
                } else {
                    AddressText(
                        address = reputation.ngoAddress,
                        showCopyIcon = false,
                    )
                }
                CheckIcon(tint = LivanaColors.Primary)
            }
            if (reputation.orgName != null) {
                Row(
                    modifier = Modifier.padding(top = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AddressText(
                        address = reputation.ngoAddress,
                        showCopyIcon = true,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stat tiles
// ---------------------------------------------------------------------------

@Composable
private fun StatTiles(
    reputation: NgoReputation,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
    ) {
        StatTile(
            value = reputation.totalSbts.toString(),
            label = "SBTs",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = reputation.totalAmountReleased.formatWhole(),
            label = "released",
            valueColor = LivanaColors.Primary,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = reputation.poolCount.toString(),
            label = "pools",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LivanaColors.Text,
) {
    LivanaCard(
        modifier = modifier,
        style = LivanaCardStyle.Flat,
        contentPadding = PaddingValues(StatTilePadding),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                color = valueColor,
                style = MetricStyles.Display.copy(fontSize = 23.sp, lineHeight = 28.sp),
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

// ---------------------------------------------------------------------------
// SBT history row
// ---------------------------------------------------------------------------

@Composable
private fun SbtHistoryRow(sbt: SbtMint) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HistoryRowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(tint = IconChipTint.Gold) {
            MedalIcon(tint = LocalContentColor.current)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${sbt.amount.formatWhole()} released",
                color = LivanaColors.Primary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "from ${truncateAddress(sbt.poolAddress)} · ${sbt.blockTimestamp.toShortDate()}",
                modifier = Modifier.padding(top = 2.dp),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            text = "tx ↗",
            modifier = Modifier.clickable(role = Role.Button) {
                clipboard.setText(AnnotatedString(sbt.txHash))
            },
            color = LivanaColors.Primary,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

// ---------------------------------------------------------------------------
// Section title
// ---------------------------------------------------------------------------

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
    ) {
        Box(
            modifier = Modifier
                .width(AccentRuleWidth)
                .height(AccentRuleHeight)
                .background(LivanaColors.Primary),
        )
        Text(
            text = text,
            color = LivanaColors.Text,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

// ---------------------------------------------------------------------------
// SBT caption footnote
// ---------------------------------------------------------------------------

@Composable
private fun SbtCaptionFootnote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = Spacing.S4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
        verticalAlignment = Alignment.Top,
    ) {
        ShieldCheckIcon(
            tint = LivanaColors.Primary,
            modifier = Modifier
                .size(ComponentDimens.SmallIconSize)
                .padding(top = 1.dp),
        )
        Text(
            text = "SBTs are non-transferable soulbound tokens — permanent, on-chain proof of verified aid delivered by this organization.",
            color = LivanaColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}



// ---------------------------------------------------------------------------
// Loading / state helpers
// ---------------------------------------------------------------------------

@Composable
private fun ProfileLoadingSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.ScreenHorizontal,
            top = Spacing.S4,
            end = Spacing.ScreenHorizontal,
            bottom = Spacing.S24,
        ),
    ) {
        item {
            // Header skeleton
            LivanaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SkeletonBlock(
                        modifier = Modifier.size(AvatarSize),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .padding(top = Spacing.S12)
                            .fillMaxWidth(0.5f)
                            .height(18.dp),
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .padding(top = Spacing.S8)
                            .fillMaxWidth(0.35f)
                            .height(12.dp),
                    )
                }
            }
        }
        item {
            // Stat tiles skeleton
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.S12),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
            ) {
                repeat(3) {
                    StatCardSkeleton(modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            // History skeleton
            Spacer(modifier = Modifier.height(Spacing.S24))
            LivanaCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
            ) {
                Column {
                    repeat(3) {
                        ListRowSkeleton()
                        if (it < 2) DividerLine()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingMoreIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.S16),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(ComponentDimens.SmallIconSize),
            color = LivanaColors.Primary,
            strokeWidth = Borders.Spinner,
        )
        Spacer(modifier = Modifier.width(Spacing.S8))
        Text(
            text = "Loading more",
            color = LivanaColors.Primary,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}



@Composable
private fun ProfileStateContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.ScreenHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private val AvatarSize = 64.dp
private val StatTilePadding = 15.dp
private val HistoryRowVerticalPadding = 13.dp
private val AccentRuleWidth = 4.dp
private val AccentRuleHeight = 20.dp
private const val PaginationThreshold = 3
private const val IconStrokeRatio = 0.1f

/**
 * Number of LazyColumn items before the history rows (header, stats, section title).
 * Used to offset pagination threshold calculation.
 */
private const val HeaderItemCount = 3

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun NgoProfileLoadedWithOrgNamePreview() {
    LivanaTheme {
        NgoProfileScreen(
            state = NgoProfileUiState.Content(
                reputation = sampleReputation(orgName = "Clean Water Foundation"),
                history = sampleHistory(),
                isLoadingMore = false,
                endReached = true,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun NgoProfileLoadedNoOrgNamePreview() {
    LivanaTheme {
        NgoProfileScreen(
            state = NgoProfileUiState.Content(
                reputation = sampleReputation(orgName = null),
                history = sampleHistory(),
                isLoadingMore = false,
                endReached = true,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun NgoProfileEmptyHistoryPreview() {
    LivanaTheme {
        NgoProfileScreen(
            state = NgoProfileUiState.Content(
                reputation = sampleReputation(orgName = "New Cause Foundation"),
                history = emptyList(),
                isLoadingMore = false,
                endReached = true,
            ),
        )
    }
}

private fun sampleReputation(orgName: String?) = NgoReputation(
    ngoAddress = "0x12ab3c4d5e6f7890abcdef1234567890abcd7f9c",
    orgName = orgName,
    totalSbts = 5,
    totalAmountReleased = Usdc(BigInteger.valueOf(15_000_000_000)),
    poolCount = 3,
)

private fun sampleHistory(): List<SbtMint> = listOf(
    SbtMint(tokenId = 1, poolAddress = "0xaabb11223344556677889900aabbccddeeff0011", amount = Usdc(BigInteger.valueOf(5_000_000_000)), txHash = "0xdeadbeef01", blockTimestamp = "2026-06-12T10:00:00.000+00:00"),
    SbtMint(tokenId = 2, poolAddress = "0xccdd22334455667788990011aabbccddeeff2233", amount = Usdc(BigInteger.valueOf(4_200_000_000)), txHash = "0xdeadbeef02", blockTimestamp = "2026-05-28T10:00:00.000+00:00"),
    SbtMint(tokenId = 3, poolAddress = "0xeeff33445566778899001122aabbccddeeff4455", amount = Usdc(BigInteger.valueOf(3_800_000_000)), txHash = "0xdeadbeef03", blockTimestamp = "2026-05-04T10:00:00.000+00:00"),
)
