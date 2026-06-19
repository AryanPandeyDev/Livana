package com.livana.app.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livana.app.core.auth.AuthState
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.LivanaChip
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Radii
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.UserProfile
import com.livana.app.core.ui.ErrorState
import com.livana.app.core.ui.OfflineState
import com.livana.app.core.ui.PoolCardSkeleton
import com.livana.app.core.ui.SkeletonBlock
import com.livana.app.core.ui.StatCardSkeleton
import com.livana.app.feature.home.components.FeaturedCausesSection
import com.livana.app.feature.home.components.HomeStatTiles
import com.livana.app.feature.home.components.ImpactHeroCard
import com.livana.app.feature.home.components.TopNgosSection

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenPool: (String) -> Unit = {},
    onOpenNgo: (String) -> Unit = {},
    onSeeAllNgos: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas)
            .statusBarsPadding(),
    ) {
        HomeTopBar(authState = authState, onSignIn = onSignIn)
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
                onOpenNgo = onOpenNgo,
                onSeeAllNgos = onSeeAllNgos,
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    authState: AuthState,
    onSignIn: () -> Unit,
) {
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
        when (authState) {
            is AuthState.SignedIn -> UserAvatar(user = authState.user)
            // Loading or SignedOut: offer sign-in. (No session yet, so show the chip.)
            else -> LivanaChip(
                text = "Sign in",
                selected = false,
                onClick = onSignIn,
            )
        }
    }
}

@Composable
private fun UserAvatar(user: UserProfile) {
    // Avatar is the derived gradient identicon — wallet address when linked, else a deterministic
    // fallback from the user id (a generic, non-placeholder avatar). No stock imagery.
    AddressAvatar(
        address = user.walletAddress ?: user.id,
        modifier = Modifier.size(ComponentDimens.IconButtonVisualSize),
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onOpenPool: (String) -> Unit,
    onOpenNgo: (String) -> Unit,
    onSeeAllNgos: () -> Unit,
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
            HomeStatTiles(
                activePools = state.activePools,
                verifiedNgos = state.verifiedNgos,
                totalPools = state.totalPools,
            )
        }
        FeaturedCausesSection(
            pools = state.featuredPools,
            onOpenPool = onOpenPool,
        )
        // Top NGOs degrades gracefully: hidden entirely when there are no entries.
        if (state.topNgos.isNotEmpty()) {
            TopNgosSection(
                ngos = state.topNgos,
                onOpenNgo = onOpenNgo,
                onSeeAll = onSeeAllNgos,
            )
        }
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
                        SkeletonBlock(
                            Modifier.size(ComponentDimens.IconButtonVisualSize),
                            shape = RoundedCornerShape(Radii.Input),
                        )
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
