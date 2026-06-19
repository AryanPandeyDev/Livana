package com.livana.app.feature.pooldetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livana.app.core.common.toShortDate
import com.livana.app.core.designsystem.component.DividerLine
import com.livana.app.core.designsystem.component.BackChevronIcon
import com.livana.app.core.designsystem.component.DocumentIcon
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.StatusPill
import com.livana.app.core.designsystem.component.StatusPillKind
import com.livana.app.core.designsystem.component.ShieldIcon
import com.livana.app.core.designsystem.component.truncateAddress
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.Proof
import com.livana.app.core.model.Usdc
import com.livana.app.core.ui.EmptyState
import com.livana.app.core.ui.ErrorState
import com.livana.app.core.ui.ListRowSkeleton
import com.livana.app.core.ui.OfflineState
import com.livana.app.feature.pooldetail.state.PoolProofsUiState
import com.livana.app.feature.pooldetail.viewmodel.PoolProofsViewModel
import java.math.BigInteger
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@Composable
fun PoolProofsScreen(
    viewModel: PoolProofsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PoolProofsScreen(
        state = state,
        address = viewModel.address,
        onBack = onBack,
        onLoadMore = viewModel::loadNextPage,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun PoolProofsScreen(
    state: PoolProofsUiState,
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
        ProofsAppBar(
            poolAddress = address,
            onBack = onBack,
        )
        when (state) {
            is PoolProofsUiState.Loading -> ProofsLoadingList()

            is PoolProofsUiState.Content -> ProofsContentList(
                state = state,
                onLoadMore = onLoadMore,
            )

            is PoolProofsUiState.Empty -> ProofsStateContainer {
                EmptyState(
                    title = "No proofs submitted yet",
                    message = "Proofs of impact will appear here once the NGO submits them.",
                )
            }

            is PoolProofsUiState.Error -> ProofsStateContainer {
                ErrorState(
                    message = state.message ?: "We couldn't load proofs. Please try again.",
                    onRetry = onRetry,
                )
            }

            is PoolProofsUiState.Offline -> ProofsStateContainer {
                OfflineState(onRetry = onRetry)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// App bar
// ---------------------------------------------------------------------------

@Composable
private fun ProofsAppBar(
    poolAddress: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.S8,
                vertical = Spacing.S8,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonLivana(
            onClick = onBack,
            contentDescription = "Back",
        ) {
            BackChevronIcon()
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Proof of impact",
                color = LivanaColors.Text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = truncateAddress(poolAddress),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        // Balance spacer to match the leading icon width.
        Spacer(modifier = Modifier.width(ComponentDimens.IconChipSize))
    }
}



// ---------------------------------------------------------------------------
// Content list with infinite scroll
// ---------------------------------------------------------------------------

@Composable
private fun ProofsContentList(
    state: PoolProofsUiState.Content,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // All proofs render inside one grouped-card lazy item (per the mockup), so a
            // per-proof index threshold doesn't apply. Trigger when the last lazy item
            // (card / footnote / loading) is reached; VM guards prevent over-fetching.
            lastVisibleIndex >= layoutInfo.totalItemsCount - 1
        }
            .distinctUntilChanged()
            .filter { atEnd -> atEnd }
            .collect { onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.ScreenHorizontal,
            top = Spacing.S8,
            end = Spacing.ScreenHorizontal,
            bottom = Spacing.S24,
        ),
    ) {
        // Grouped card: all proof rows inside a single rounded white card per the mockup.
        item(key = "proof-card") {
            LivanaCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
            ) {
                Column {
                    state.proofs.forEachIndexed { index, proof ->
                        ProofFullRow(proof = proof)
                        if (index < state.proofs.lastIndex) {
                            DividerLine()
                        }
                    }
                }
            }
        }

        // Trust footnote below the card.
        item(key = "trust-footnote") {
            TrustFootnote(
                modifier = Modifier.padding(top = Spacing.S16),
            )
        }

        if (state.isLoadingMore) {
            item(key = "loading-more") {
                LoadingMoreIndicator()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Proof row (faithful to 08-pool-proofs.html)
// ---------------------------------------------------------------------------

@Composable
private fun ProofFullRow(proof: Proof) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ProofRowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(tint = IconChipTint.Jade) {
            DocumentIcon(tint = LocalContentColor.current)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${proof.amount.formatWhole()} claimed",
                color = LivanaColors.Text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "Submitted ${proof.submittedAt.toShortDate()}",
                modifier = Modifier.padding(top = 2.dp),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        StatusPill(
            kind = if (proof.released) StatusPillKind.Released else StatusPillKind.Pending,
            label = if (proof.released) "Released" else "Pending",
        )
    }
}

// ---------------------------------------------------------------------------
// Trust footnote
// ---------------------------------------------------------------------------

@Composable
private fun TrustFootnote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = Spacing.S4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
        verticalAlignment = Alignment.Top,
    ) {
        ShieldIcon(
            tint = LivanaColors.Primary,
            modifier = Modifier
                .size(ComponentDimens.SmallIconSize)
                .padding(top = 1.dp),
        )
        Text(
            text = "Each proof links to a stored document and is released only after admin verification.",
            color = LivanaColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}



// ---------------------------------------------------------------------------
// Loading / state helpers
// ---------------------------------------------------------------------------

@Composable
private fun ProofsLoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.ScreenHorizontal,
            end = Spacing.ScreenHorizontal,
            bottom = Spacing.S24,
        ),
    ) {
        item {
            LivanaCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
            ) {
                Column {
                    repeat(5) {
                        ListRowSkeleton()
                        if (it < 4) DividerLine()
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
private fun ProofsStateContainer(content: @Composable () -> Unit) {
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

private val ProofRowVerticalPadding = 14.dp

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun PoolProofsLoadedPreview() {
    LivanaTheme {
        PoolProofsScreen(
            state = PoolProofsUiState.Content(
                proofs = sampleProofs(),
                isLoadingMore = false,
                endReached = false,
            ),
            address = "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun PoolProofsEmptyPreview() {
    LivanaTheme {
        PoolProofsScreen(
            state = PoolProofsUiState.Empty,
            address = "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
        )
    }
}

private fun sampleProofs(): List<Proof> = listOf(
    Proof(proofId = 1, ipfsCid = "Qm1", amount = Usdc(BigInteger.valueOf(500_000_000)), released = true, submittedAt = "2026-06-07T10:00:00.000+00:00", releasedAt = "2026-06-08T14:00:00.000+00:00"),
    Proof(proofId = 2, ipfsCid = "Qm2", amount = Usdc(BigInteger.valueOf(800_000_000)), released = false, submittedAt = "2026-06-09T10:00:00.000+00:00", releasedAt = null),
    Proof(proofId = 3, ipfsCid = "Qm3", amount = Usdc(BigInteger.valueOf(1_200_000_000)), released = true, submittedAt = "2026-06-12T10:00:00.000+00:00", releasedAt = "2026-06-13T09:00:00.000+00:00"),
    Proof(proofId = 4, ipfsCid = "Qm4", amount = Usdc(BigInteger.valueOf(650_000_000)), released = false, submittedAt = "2026-06-15T10:00:00.000+00:00", releasedAt = null),
)
