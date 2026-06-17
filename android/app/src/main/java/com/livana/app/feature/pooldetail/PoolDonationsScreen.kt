package com.livana.app.feature.pooldetail

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livana.app.core.common.toRelativeTime
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.AddressText
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.truncateAddress
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolDonation
import com.livana.app.core.model.Usdc
import com.livana.app.core.ui.EmptyState
import com.livana.app.core.ui.ErrorState
import com.livana.app.core.ui.ListRowSkeleton
import com.livana.app.core.ui.OfflineState
import java.math.BigInteger
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@Composable
fun PoolDonationsScreen(
    viewModel: PoolDonationsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PoolDonationsScreen(
        state = state,
        address = viewModel.address,
        onBack = onBack,
        onLoadMore = viewModel::loadNextPage,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun PoolDonationsScreen(
    state: PoolDonationsUiState,
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
        DonationsAppBar(
            poolAddress = address,
            onBack = onBack,
        )
        when (state) {
            is PoolDonationsUiState.Loading -> DonationsLoadingList()

            is PoolDonationsUiState.Content -> DonationsContentList(
                state = state,
                onLoadMore = onLoadMore,
            )

            is PoolDonationsUiState.Empty -> DonationsStateContainer {
                EmptyState(
                    title = "No donations yet",
                    message = "Be the first to give.",
                )
            }

            is PoolDonationsUiState.Error -> DonationsStateContainer {
                ErrorState(
                    message = state.message ?: "We couldn't load donations. Please try again.",
                    onRetry = onRetry,
                )
            }

            is PoolDonationsUiState.Offline -> DonationsStateContainer {
                OfflineState(onRetry = onRetry)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// App bar
// ---------------------------------------------------------------------------

@Composable
private fun DonationsAppBar(
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
            BackChevron()
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Donations",
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

@Composable
private fun BackChevron() {
    val color = LivanaColors.Text
    androidx.compose.foundation.Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val sw = size.minDimension * 0.1f
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.18f), androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * 0.5f), sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * 0.5f), androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.82f), sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------
// Content list with infinite scroll
// ---------------------------------------------------------------------------

@Composable
private fun DonationsContentList(
    state: PoolDonationsUiState.Content,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastIndex -> lastIndex >= state.donations.size - PaginationThreshold }
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
        items(
            items = state.donations,
            key = { donation -> donation.txHash },
        ) { donation ->
            DonationFullRow(donation = donation)
            DividerLine()
        }
        if (state.isLoadingMore) {
            item(key = "loading-more") {
                LoadingMoreIndicator()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Donation row (faithful to 07-pool-donations.html)
// ---------------------------------------------------------------------------

@Composable
private fun DonationFullRow(donation: PoolDonation) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DonationRowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AddressAvatar(
            address = donation.donorAddress,
            modifier = Modifier.size(DonationAvatarSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            AddressText(
                address = donation.donorAddress,
                showCopyIcon = false,
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = donation.blockTimestamp.toRelativeTime(),
                    color = LivanaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = " · ",
                    color = LivanaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "tx ↗",
                    modifier = Modifier.clickable(role = Role.Button) {
                        clipboard.setText(AnnotatedString(donation.txHash))
                    },
                    color = LivanaColors.Primary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Text(
            text = donation.amount.formatWhole(),
            color = LivanaColors.Primary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

// ---------------------------------------------------------------------------
// Loading / state helpers
// ---------------------------------------------------------------------------

@Composable
private fun DonationsLoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.ScreenHorizontal,
            end = Spacing.ScreenHorizontal,
            bottom = Spacing.S24,
        ),
    ) {
        items(8) {
            ListRowSkeleton()
            DividerLine()
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
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Borders.Hairline)
            .background(LivanaColors.Hairline),
    )
}

@Composable
private fun DonationsStateContainer(content: @Composable () -> Unit) {
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

private val DonationAvatarSize = 34.dp
private val DonationRowVerticalPadding = 13.dp
private const val PaginationThreshold = 3

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun PoolDonationsLoadedPreview() {
    LivanaTheme {
        PoolDonationsScreen(
            state = PoolDonationsUiState.Content(
                donations = sampleDonations(),
                isLoadingMore = true,
                endReached = false,
            ),
            address = "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun PoolDonationsEmptyPreview() {
    LivanaTheme {
        PoolDonationsScreen(
            state = PoolDonationsUiState.Empty,
            address = "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
        )
    }
}

private fun sampleDonations(): List<PoolDonation> = listOf(
    PoolDonation("0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", Usdc(BigInteger.valueOf(2_000_000_000)), "0x2bf5a", "2026-06-07T12:00:00.000+00:00"),
    PoolDonation("0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc", Usdc(BigInteger.valueOf(500_000_000)), "0x2bf6b", "2026-06-07T13:00:00.000+00:00"),
    PoolDonation("0x70d2f0f47f8c2d8dddf1a1f3d6f1bd8e8b1c1f08", Usdc(BigInteger.valueOf(250_000_000)), "0x2bf7c", "2026-06-08T09:00:00.000+00:00"),
)
