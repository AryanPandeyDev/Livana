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
import com.livana.app.core.designsystem.component.BackChevronIcon
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.CheckIcon
import com.livana.app.core.designsystem.component.ChevronRightIcon
import com.livana.app.core.designsystem.component.DocumentIcon
import com.livana.app.core.designsystem.component.HeartIcon
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.LivanaButtonState
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.component.LivanaProgress
import com.livana.app.core.designsystem.component.LivanaTabs
import com.livana.app.core.designsystem.component.DividerLine
import com.livana.app.core.designsystem.component.LivanaTextButton
import com.livana.app.core.designsystem.component.LivanaTonalButton
import com.livana.app.feature.pooldetail.components.DonateDock
import com.livana.app.feature.pooldetail.components.PoolCreatorRow
import com.livana.app.feature.pooldetail.components.PoolDetailTabs
import com.livana.app.feature.pooldetail.components.PoolHero
import com.livana.app.feature.pooldetail.components.PoolProgressCard
import com.livana.app.core.designsystem.component.ShareIcon
import com.livana.app.core.designsystem.component.ShieldIcon
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
    onOpenNgo: (String) -> Unit = {},
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
    onOpenNgo: (String) -> Unit = {},
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
    onOpenNgo: (String) -> Unit,
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
                PoolProgressCard(pool = pool)
                PoolCreatorRow(
                    pool = pool,
                    onOpenNgo = { onOpenNgo(pool.creatorAddress) },
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
            BackChevronIcon(tint = LivanaColors.Primary)
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

internal val PoolDetailHeroHeight = Spacing.S40 * 6 + Spacing.S24 + Spacing.S4
internal val PoolDetailTabs = listOf("About", "Donations", "Proofs")
internal const val CollapsedDescriptionLines = 4
internal const val PreviewRows = 3
internal const val ProgressScale = 4
internal const val IconStrokeRatio = 0.1f
internal const val AvatarMarkAlpha = 0.82f

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
        orgName = "Clean Water Foundation",
        totalSbts = 5,
        totalAmountReleased = 15_000_000_000L.toUsdcPreview(),
        poolCount = 3,
    ),
)

private fun Long.toUsdcPreview(): Usdc = Usdc(BigInteger.valueOf(this))
