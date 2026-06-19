package com.livana.app.feature.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livana.app.core.designsystem.component.DividerLine
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.DonorLeaderboardEntry
import com.livana.app.core.model.NgoLeaderboardEntry
import com.livana.app.core.model.Usdc
import com.livana.app.core.ui.EmptyState
import com.livana.app.core.ui.ErrorState
import com.livana.app.core.ui.ListRowSkeleton
import com.livana.app.core.ui.OfflineState
import com.livana.app.feature.leaderboard.components.BoardsSegmentToggle
import com.livana.app.feature.leaderboard.components.DonorLeaderboardRow
import com.livana.app.feature.leaderboard.components.NgoLeaderboardRow
import java.math.BigInteger

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@Composable
fun BoardsScreen(
    onOpenNgo: (String) -> Unit,
    viewModel: BoardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BoardsScreen(
        state = state,
        onOpenNgo = onOpenNgo,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun BoardsScreen(
    state: BoardsUiState,
    onOpenNgo: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    var segment by rememberSaveable { mutableStateOf(BoardsSegment.Donors) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.padding(
                start = Spacing.ScreenHorizontal,
                end = Spacing.ScreenHorizontal,
                top = Spacing.S8,
            ),
        ) {
            BoardsSegmentToggle(
                selected = segment,
                onSelect = { segment = it },
                modifier = Modifier.padding(bottom = Spacing.S16),
            )
            BoardsHeader(segment = segment)
        }

        when (state) {
            is BoardsUiState.Loading -> BoardsLoadingList()

            is BoardsUiState.Content -> when (segment) {
                BoardsSegment.Donors -> DonorsList(donors = state.donors)
                BoardsSegment.Ngos -> NgosList(ngos = state.ngos, onOpenNgo = onOpenNgo)
            }

            is BoardsUiState.Empty -> BoardsStateContainer {
                EmptyState(
                    title = "No entries yet",
                    message = "Leaderboards fill up as donations and verified releases roll in.",
                )
            }

            is BoardsUiState.Error -> BoardsStateContainer {
                ErrorState(
                    message = state.message ?: "We couldn't load the leaderboards. Please try again.",
                    onRetry = onRetry,
                )
            }

            is BoardsUiState.Offline -> BoardsStateContainer {
                OfflineState(onRetry = onRetry)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header (title + subtitle change per segment)
// ---------------------------------------------------------------------------

@Composable
private fun BoardsHeader(segment: BoardsSegment) {
    val (title, subtitle) = when (segment) {
        BoardsSegment.Donors -> "Top donors" to "Recognizing generous giving."
        BoardsSegment.Ngos -> "Top NGOs" to "Ranked by verified impact."
    }
    Column {
        Text(
            text = title,
            color = LivanaColors.Text,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 25.sp),
        )
        Text(
            text = subtitle,
            modifier = Modifier.padding(top = 2.dp),
            color = LivanaColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ---------------------------------------------------------------------------
// Donors list (09-donor-leaderboard.html): top-3 podium + grouped numbered list
// ---------------------------------------------------------------------------

@Composable
private fun DonorsList(donors: List<DonorLeaderboardEntry>) {
    if (donors.isEmpty()) {
        BoardsStateContainer {
            EmptyState(
                title = "No entries yet",
                message = "Donor rankings appear once donations start flowing.",
            )
        }
        return
    }

    val podium = donors.take(PodiumSize)
    val rest = donors.drop(PodiumSize)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listContentPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.S12),
    ) {
        itemsIndexed(
            items = podium,
            key = { index, entry -> "podium-${entry.donorAddress}-$index" },
        ) { index, entry ->
            LivanaCard(
                modifier = Modifier.fillMaxWidth(),
                style = LivanaCardStyle.Flat,
                contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = 14.dp),
            ) {
                DonorLeaderboardRow(entry = entry, rank = index + 1, podium = true)
            }
        }

        if (rest.isNotEmpty()) {
            item(key = "donor-list-card") {
                LivanaCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
                ) {
                    Column {
                        rest.forEachIndexed { index, entry ->
                            DonorLeaderboardRow(
                                entry = entry,
                                rank = PodiumSize + index + 1,
                            )
                            if (index < rest.lastIndex) {
                                DividerLine()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// NGOs list (10-ngo-leaderboard.html): single grouped card of tappable rows
// ---------------------------------------------------------------------------

@Composable
private fun NgosList(
    ngos: List<NgoLeaderboardEntry>,
    onOpenNgo: (String) -> Unit,
) {
    if (ngos.isEmpty()) {
        BoardsStateContainer {
            EmptyState(
                title = "No entries yet",
                message = "Verified NGOs appear here as they deliver impact.",
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listContentPadding(),
    ) {
        item(key = "ngo-list-card") {
            LivanaCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
            ) {
                Column {
                    ngos.forEachIndexed { index, entry ->
                        NgoLeaderboardRow(
                            entry = entry,
                            onClick = { onOpenNgo(entry.ngoAddress) },
                        )
                        if (index < ngos.lastIndex) {
                            DividerLine()
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Loading / state helpers
// ---------------------------------------------------------------------------

@Composable
private fun BoardsLoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listContentPadding(),
    ) {
        item {
            LivanaCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
            ) {
                Column {
                    repeat(SkeletonRows) {
                        ListRowSkeleton()
                        if (it < SkeletonRows - 1) DividerLine()
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardsStateContainer(content: @Composable () -> Unit) {
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
private fun listContentPadding() = PaddingValues(
    start = Spacing.ScreenHorizontal,
    top = Spacing.S16,
    end = Spacing.ScreenHorizontal,
    bottom = Spacing.S24,
)

private const val PodiumSize = 3
private const val SkeletonRows = 6

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun BoardsContentPreview() {
    LivanaTheme {
        BoardsScreen(
            state = BoardsUiState.Content(
                donors = sampleDonors(),
                ngos = sampleNgos(),
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun BoardsEmptyPreview() {
    LivanaTheme {
        BoardsScreen(state = BoardsUiState.Empty)
    }
}

private fun sampleDonors(): List<DonorLeaderboardEntry> = listOf(
    DonorLeaderboardEntry("0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", Usdc(BigInteger.valueOf(12_400_000_000)), 28),
    DonorLeaderboardEntry("0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc", Usdc(BigInteger.valueOf(9_800_000_000)), 19),
    DonorLeaderboardEntry("0x70d2b7c19352bb76e4409858ff5a18c1748c1f08", Usdc(BigInteger.valueOf(7_250_000_000)), 15),
    DonorLeaderboardEntry("0x18af2c9876a543210fedcba9876543210fed6b21", Usdc(BigInteger.valueOf(5_600_000_000)), 7),
    DonorLeaderboardEntry("0xab33c45deef0112233445566778899aabbcc0c4e", Usdc(BigInteger.valueOf(4_300_000_000)), 11),
)

private fun sampleNgos(): List<NgoLeaderboardEntry> = listOf(
    NgoLeaderboardEntry("0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc", "Clean Water Foundation", 5, Usdc(BigInteger.valueOf(15_000_000_000)), 3, 1),
    NgoLeaderboardEntry("0x70d2b7c19352bb76e4409858ff5a18c1748c1f08", "Shelter Now", 3, Usdc(BigInteger.valueOf(8_400_000_000)), 2, 2),
    NgoLeaderboardEntry("0x18af2c9876a543210fedcba9876543210fed6b21", null, 1, Usdc(BigInteger.valueOf(2_300_000_000)), 1, 3),
)
