package com.livana.app.feature.pooldetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.livana.app.BuildConfig
import com.livana.app.core.designsystem.component.BackChevronIcon
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.LivanaIconButtonStyle
import com.livana.app.core.designsystem.component.ShareIcon
import com.livana.app.core.designsystem.component.StatusPill
import com.livana.app.core.designsystem.component.StatusPillKind
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolDetail
import com.livana.app.feature.pooldetail.PoolDetailHeroHeight
import com.livana.app.feature.pooldetail.AvatarMarkAlpha

@Composable
fun PoolHero(
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
                style = LivanaIconButtonStyle.Glass,
            ) {
                BackChevronIcon(tint = LivanaColors.Text)
            }
            IconButtonLivana(
                onClick = {},
                contentDescription = "Share",
                style = LivanaIconButtonStyle.Glass,
            ) {
                ShareIcon(tint = LivanaColors.Text)
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
