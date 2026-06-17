package com.livana.app.feature.pooldetail

import android.app.Activity
import com.livana.app.core.common.toRelativeTime
import com.livana.app.core.common.toShortDate
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.livana.app.BuildConfig
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.AddressText
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.LivanaButtonState
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.component.LivanaProgress
import com.livana.app.core.designsystem.component.LivanaTabs
import com.livana.app.core.designsystem.component.LivanaTextButton
import com.livana.app.core.designsystem.component.LivanaTonalButton
import com.livana.app.core.designsystem.component.StatusPill
import com.livana.app.core.designsystem.component.StatusPillKind
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.CardShape
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.core.designsystem.theme.MetricStyles
import com.livana.app.core.designsystem.theme.Radii
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.NgoReputation
import com.livana.app.core.model.PoolDetail
import com.livana.app.core.model.PoolDonation
import com.livana.app.core.model.Proof
import com.livana.app.core.model.Region
import com.livana.app.core.model.Usdc
import com.livana.app.core.ui.DetailScreenSkeleton
import com.livana.app.core.ui.EmptyState
import com.livana.app.core.ui.ErrorState
import com.livana.app.core.ui.OfflineState
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

@Composable
fun PoolDetailScreen(
    viewModel: PoolDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onDonate: () -> Unit = {},
    onOpenNgo: () -> Unit = {},
    onSeeAllDonations: () -> Unit = {},
    onSeeAllProofs: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PoolDetailScreen(
        state = state,
        onBack = onBack,
        onDonate = onDonate,
        onOpenNgo = onOpenNgo,
        onSeeAllDonations = onSeeAllDonations,
        onSeeAllProofs = onSeeAllProofs,
        onRetry = viewModel::retry,
    )
}

@Composable
fun PoolDetailScreen(
    state: PoolDetailUiState,
    onBack: () -> Unit = {},
    onDonate: () -> Unit = {},
    onOpenNgo: () -> Unit = {},
    onSeeAllDonations: () -> Unit = {},
    onSeeAllProofs: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    when (state) {
        PoolDetailUiState.Loading -> PoolDetailLoading(onBack = onBack)
        is PoolDetailUiState.Content -> PoolDetailContent(
            pool = state.pool,
            onBack = onBack,
            onDonate = onDonate,
            onOpenNgo = onOpenNgo,
            onSeeAllDonations = onSeeAllDonations,
            onSeeAllProofs = onSeeAllProofs,
        )

        is PoolDetailUiState.Error -> PoolDetailStateScaffold(onBack = onBack) {
            ErrorState(
                message = state.message ?: "We couldn't load this cause. Please try again.",
                onRetry = onRetry,
            )
        }

        PoolDetailUiState.Offline -> PoolDetailStateScaffold(onBack = onBack) {
            OfflineState(onRetry = onRetry)
        }

        is PoolDetailUiState.NotFound -> PoolDetailStateScaffold(onBack = onBack) {
            EmptyState(
                title = "Cause unavailable",
                message = state.message,
            )
        }
    }
}

@Composable
private fun PoolDetailContent(
    pool: PoolDetail,
    onBack: () -> Unit,
    onDonate: () -> Unit,
    onOpenNgo: () -> Unit,
    onSeeAllDonations: () -> Unit,
    onSeeAllProofs: () -> Unit,
) {
    PoolDetailStatusBarEffect()
    Scaffold(
        containerColor = LivanaColors.Canvas,
        bottomBar = {
            DonateDock(
                isPaused = pool.isPaused,
                onDonate = onDonate,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            PoolHero(pool = pool, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = -Spacing.S32)
                    .padding(horizontal = Spacing.ScreenHorizontal),
            ) {
                ProgressCard(pool = pool)
                CreatorRow(
                    pool = pool,
                    onOpenNgo = onOpenNgo,
                )
                DividerLine()
                PoolDetailTabs(
                    pool = pool,
                    onSeeAllDonations = onSeeAllDonations,
                    onSeeAllProofs = onSeeAllProofs,
                )
                Spacer(modifier = Modifier.height(Spacing.S32 + Spacing.S24))
            }
        }
    }
}

@Composable
private fun PoolHero(
    pool: PoolDetail,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PoolDetailHeroHeight)
            .background(Brush.linearGradient(listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroC))),
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
                petalColor = LivanaColors.OnPrimary.copy(alpha = AvatarMarkAlpha),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = Spacing.S16,
                    top = Spacing.S8,
                    end = Spacing.S16,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButtonLivana(
                onClick = onBack,
                contentDescription = "Back",
                style = com.livana.app.core.designsystem.component.LivanaIconButtonStyle.Glass,
            ) {
                BackChevronIcon()
            }
            IconButtonLivana(
                onClick = {},
                contentDescription = "Share",
                style = com.livana.app.core.designsystem.component.LivanaIconButtonStyle.Glass,
            ) {
                ShareIcon()
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = Spacing.ScreenHorizontal,
                    end = Spacing.ScreenHorizontal,
                    bottom = Spacing.S24,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.S8),
        ) {
            StatusPill(
                kind = StatusPillKind.Region,
                label = pool.region?.display ?: "Global",
            )
            Text(
                text = pool.title,
                color = LivanaColors.OnPrimary,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgressCard(pool: PoolDetail) {
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

@Composable
private fun CreatorRow(
    pool: PoolDetail,
    onOpenNgo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpenNgo)
            .padding(vertical = Spacing.S16, horizontal = Spacing.S4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AddressAvatar(
            address = pool.creatorAddress,
            modifier = Modifier.size(ComponentDimens.MinimumTouchTarget),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.S4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AddressText(
                    address = pool.creatorAddress,
                    modifier = Modifier.weight(1f, fill = false),
                    showCopyIcon = false,
                )
                CheckGlyph(color = LivanaColors.Primary)
            }
            Text(
                text = "${pool.creatorReputation.totalSbts} SBTs - ${pool.creatorReputation.totalAmountReleased.formatWhole()} released",
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        ChevronRightIcon(color = LivanaColors.TextMuted)
    }
}

@Composable
private fun PoolDetailTabs(
    pool: PoolDetail,
    onSeeAllDonations: () -> Unit,
    onSeeAllProofs: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    LivanaTabs(
        tabs = PoolDetailTabs,
        selectedIndex = selectedTab,
        onTabSelected = { selectedTab = it },
        modifier = Modifier.padding(top = Spacing.S16),
    )
    when (selectedTab) {
        0 -> AboutPanel(pool = pool)
        1 -> DonationsPanel(
            donations = pool.recentDonations,
            onSeeAllDonations = onSeeAllDonations,
        )

        2 -> ProofsPanel(
            proofs = pool.recentProofs,
            onSeeAllProofs = onSeeAllProofs,
        )
    }
}

@Composable
private fun AboutPanel(pool: PoolDetail) {
    var expanded by remember(pool.description) { mutableStateOf(false) }
    Column(modifier = Modifier.padding(top = Spacing.S16)) {
        Text(
            text = pool.description,
            color = LivanaColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (expanded) Int.MAX_VALUE else CollapsedDescriptionLines,
            overflow = TextOverflow.Ellipsis,
        )
        LivanaTextButton(
            text = if (expanded) "Show less" else "Read more",
            onClick = { expanded = !expanded },
            modifier = Modifier.padding(top = Spacing.S4),
        )
        Row(
            modifier = Modifier.padding(top = Spacing.S12),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
            verticalAlignment = Alignment.Top,
        ) {
            ShieldIcon(color = LivanaColors.Primary)
            Text(
                text = "Funds are held in an on-chain escrow and only released after admin-verified proof.",
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DonationsPanel(
    donations: List<PoolDonation>,
    onSeeAllDonations: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = Spacing.S16)) {
        if (donations.isEmpty()) {
            Text(
                text = "No donations yet",
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            donations.take(PreviewRows).forEach { donation ->
                DonationRow(donation = donation)
            }
            LivanaTextButton(
                text = "See all donations",
                onClick = onSeeAllDonations,
                modifier = Modifier.padding(top = Spacing.S4),
            )
        }
    }
}

@Composable
private fun DonationRow(donation: PoolDonation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.S8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AddressAvatar(
            address = donation.donorAddress,
            modifier = Modifier.size(ComponentDimens.SkeletonAvatarSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            AddressText(
                address = donation.donorAddress,
                showCopyIcon = false,
            )
            Text(
                text = donation.blockTimestamp.toRelativeTime(),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            text = donation.amount.formatWhole(),
            color = LivanaColors.Primary,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun ProofsPanel(
    proofs: List<Proof>,
    onSeeAllProofs: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = Spacing.S16)) {
        if (proofs.isEmpty()) {
            Text(
                text = "No proofs submitted yet",
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LivanaCard(
                modifier = Modifier.fillMaxWidth(),
                style = LivanaCardStyle.Flat,
                contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
            ) {
                Column {
                    proofs.take(PreviewRows).forEachIndexed { index, proof ->
                        ProofRow(proof = proof)
                        if (index != proofs.take(PreviewRows).lastIndex) {
                            DividerLine()
                        }
                    }
                }
            }
            LivanaTextButton(
                text = "See all proofs",
                onClick = onSeeAllProofs,
                modifier = Modifier.padding(top = Spacing.S8),
            )
        }
    }
}

@Composable
private fun ProofRow(proof: Proof) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.S12),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(tint = IconChipTint.Jade) {
            DocumentIcon(color = LocalContentColor.current)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${proof.amount.formatWhole()} claimed",
                color = LivanaColors.Text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "Submitted " + proof.submittedAt.toShortDate(),
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

@Composable
private fun DonateDock(
    isPaused: Boolean,
    onDonate: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LivanaColors.Surface)
            .navigationBarsPadding()
            .padding(
                start = Spacing.ScreenHorizontal,
                end = Spacing.ScreenHorizontal,
                top = Spacing.S16,
                bottom = Spacing.S20,
            ),
    ) {
        if (isPaused) {
            LivanaTonalButton(
                text = "Donations paused",
                onClick = {},
                state = LivanaButtonState.Disabled,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LivanaPrimaryButton(
                text = "Donate",
                onClick = onDonate,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { HeartIcon(color = LivanaColors.OnPrimary) },
            )
        }
    }
}

@Composable
private fun PoolDetailLoading(onBack: () -> Unit) {
    PoolDetailStatusBarEffect()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PoolDetailHeroHeight)
                    .background(Brush.linearGradient(listOf(LivanaColors.GradHeroA, LivanaColors.GradHeroC))),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(
                            start = Spacing.S16,
                            top = Spacing.S8,
                            end = Spacing.S16,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButtonLivana(
                        onClick = onBack,
                        contentDescription = "Back",
                        style = com.livana.app.core.designsystem.component.LivanaIconButtonStyle.Glass,
                    ) {
                        BackChevronIcon()
                    }
                }
            }
            DetailScreenSkeleton(
                modifier = Modifier
                    .offset(y = -Spacing.S32)
                    .padding(horizontal = Spacing.ScreenHorizontal),
            )
        }
    }
}

@Composable
private fun PoolDetailStateScaffold(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas)
            .statusBarsPadding()
            .padding(horizontal = Spacing.ScreenHorizontal),
    ) {
        IconButtonLivana(
            onClick = onBack,
            contentDescription = "Back",
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            BackChevronIcon(color = LivanaColors.Primary)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Spacing.S40),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun PoolDetailStatusBarEffect() {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity, view) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = previous
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
private fun BackChevronIcon(color: Color = LivanaColors.Text) {
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawLine(color, Offset(size.width * 0.68f, size.height * 0.18f), Offset(size.width * 0.32f, size.height * 0.5f), stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.32f, size.height * 0.5f), Offset(size.width * 0.68f, size.height * 0.82f), stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun ShareIcon(color: Color = LivanaColors.Text) {
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.18f), Offset(size.width * 0.5f, size.height * 0.68f), stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.28f, size.height * 0.38f), Offset(size.width * 0.5f, size.height * 0.18f), stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.72f, size.height * 0.38f), Offset(size.width * 0.5f, size.height * 0.18f), stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.22f, size.height * 0.74f), Offset(size.width * 0.22f, size.height * 0.9f), stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.22f, size.height * 0.9f), Offset(size.width * 0.78f, size.height * 0.9f), stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.78f, size.height * 0.9f), Offset(size.width * 0.78f, size.height * 0.74f), stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun HeartIcon(color: Color) {
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.84f)
            cubicTo(size.width * 0.12f, size.height * 0.58f, size.width * 0.16f, size.height * 0.22f, size.width * 0.38f, size.height * 0.28f)
            cubicTo(size.width * 0.48f, size.height * 0.3f, size.width * 0.5f, size.height * 0.42f, size.width * 0.5f, size.height * 0.42f)
            cubicTo(size.width * 0.5f, size.height * 0.42f, size.width * 0.54f, size.height * 0.3f, size.width * 0.66f, size.height * 0.28f)
            cubicTo(size.width * 0.88f, size.height * 0.24f, size.width * 0.92f, size.height * 0.58f, size.width * 0.5f, size.height * 0.84f)
        }
        drawPath(path, color, style = stroke)
    }
}

@Composable
private fun CheckGlyph(color: Color) {
    Canvas(modifier = Modifier.size(ComponentDimens.StatusIconSize)) {
        val strokeWidth = size.minDimension * IconStrokeRatio
        drawLine(color, Offset(size.width * 0.18f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.75f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.42f, size.height * 0.75f), Offset(size.width * 0.84f, size.height * 0.25f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun ChevronRightIcon(color: Color) {
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val strokeWidth = size.minDimension * IconStrokeRatio
        drawLine(color, Offset(size.width * 0.34f, size.height * 0.18f), Offset(size.width * 0.68f, size.height * 0.5f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.68f, size.height * 0.5f), Offset(size.width * 0.34f, size.height * 0.82f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun ShieldIcon(color: Color) {
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.12f)
            lineTo(size.width * 0.82f, size.height * 0.26f)
            lineTo(size.width * 0.82f, size.height * 0.48f)
            cubicTo(size.width * 0.82f, size.height * 0.72f, size.width * 0.65f, size.height * 0.86f, size.width * 0.5f, size.height * 0.92f)
            cubicTo(size.width * 0.35f, size.height * 0.86f, size.width * 0.18f, size.height * 0.72f, size.width * 0.18f, size.height * 0.48f)
            lineTo(size.width * 0.18f, size.height * 0.26f)
            close()
        }
        drawPath(path, color, style = stroke)
    }
}

@Composable
private fun DocumentIcon(color: Color) {
    Canvas(modifier = Modifier.size(ComponentDimens.SmallIconSize)) {
        val stroke = Stroke(
            width = size.minDimension * IconStrokeRatio,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.1f)
            lineTo(size.width * 0.62f, size.height * 0.1f)
            lineTo(size.width * 0.8f, size.height * 0.28f)
            lineTo(size.width * 0.8f, size.height * 0.9f)
            lineTo(size.width * 0.28f, size.height * 0.9f)
            close()
            moveTo(size.width * 0.62f, size.height * 0.1f)
            lineTo(size.width * 0.62f, size.height * 0.28f)
            lineTo(size.width * 0.8f, size.height * 0.28f)
        }
        drawPath(path, color, style = stroke)
    }
}

private fun PoolDetail.donationProgress(): Float {
    if (targetAmount.atomic.signum() <= 0) return 0f
    return totalDonated.atomic.toBigDecimal()
        .divide(targetAmount.atomic.toBigDecimal(), ProgressScale, RoundingMode.DOWN)
        .coerceAtMost(BigDecimal.ONE)
        .toFloat()
}



private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val PoolDetailHeroHeight = Spacing.S40 * 6 + Spacing.S24 + Spacing.S4
private val PoolDetailTabs = listOf("About", "Donations", "Proofs")
private const val CollapsedDescriptionLines = 4
private const val PreviewRows = 3
private const val ProgressScale = 4
private const val IconStrokeRatio = 0.1f
private const val AvatarMarkAlpha = 0.82f

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun PoolDetailLoadedPreview() {
    LivanaTheme {
        PoolDetailScreen(state = PoolDetailUiState.Content(samplePoolDetail()))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun PoolDetailPausedPreview() {
    LivanaTheme {
        PoolDetailScreen(
            state = PoolDetailUiState.Content(samplePoolDetail(isPaused = true)),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun PoolDetailNotFoundPreview() {
    LivanaTheme {
        PoolDetailScreen(state = PoolDetailUiState.NotFound())
    }
}

private fun samplePoolDetail(isPaused: Boolean = false): PoolDetail = PoolDetail(
    onChainAddress = "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
    creatorAddress = "0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc",
    poolIndex = 0,
    metadataCid = "QmMetadata",
    title = "Flood Relief Fund",
    description = "Emergency aid for flood-affected communities across the region - clean water, food parcels, and temporary shelter for families who lost their homes.",
    region = Region.SOUTH_ASIA,
    coverImageCid = null,
    targetAmount = 10_000_000_000L.toUsdcPreview(),
    totalDonated = 5_000_000_000L.toUsdcPreview(),
    totalReleased = 2_000_000_000L.toUsdcPreview(),
    isPaused = isPaused,
    deployTxHash = "0x51e405490a226",
    deployBlock = 15,
    deployedAt = "2026-06-07T10:00:00.000+00:00",
    donationCount = 120,
    proofCount = 3,
    recentDonations = listOf(
        PoolDonation("0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", 2_000_000_000L.toUsdcPreview(), "0x2bf5", "2026-06-07T12:00:00.000+00:00"),
        PoolDonation("0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc", 500_000_000L.toUsdcPreview(), "0x2bf6", "2026-06-07T13:00:00.000+00:00"),
        PoolDonation("0x70d2f0f47f8c2d8dddf1a1f3d6f1bd8e8b1c1f08", 250_000_000L.toUsdcPreview(), "0x2bf7", "2026-06-08T09:00:00.000+00:00"),
    ),
    recentProofs = listOf(
        Proof(1, "QmProof1", 500_000_000L.toUsdcPreview(), true, "2026-06-07T14:00:00.000+00:00", "2026-06-07T15:00:00.000+00:00"),
        Proof(2, "QmProof2", 800_000_000L.toUsdcPreview(), false, "2026-06-09T14:00:00.000+00:00", null),
    ),
    creatorReputation = NgoReputation(
        ngoAddress = "0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc",
        totalSbts = 5,
        totalAmountReleased = 15_000_000_000L.toUsdcPreview(),
        poolCount = 3,
    ),
)

private fun Long.toUsdcPreview(): Usdc = Usdc(BigInteger.valueOf(this))
