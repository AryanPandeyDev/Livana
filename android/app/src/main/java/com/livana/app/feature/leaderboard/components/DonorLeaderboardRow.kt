package com.livana.app.feature.leaderboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.truncateAddress
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.DonorLeaderboardEntry

/**
 * One ranked donor row, faithful to 09-donor-leaderboard.html. Donors are NOT tappable
 * (no donor profile exists). When [podium] is true the row uses the top-3 treatment:
 * a tinted circular rank badge, a larger avatar and the jade display metric.
 */
@Composable
fun DonorLeaderboardRow(
    entry: DonorLeaderboardEntry,
    rank: Int,
    modifier: Modifier = Modifier,
    podium: Boolean = false,
) {
    Row(
        modifier = modifier.padding(vertical = if (podium) Spacing.S4 else RowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(if (podium) 14.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rank = rank, podium = podium)
        AddressAvatar(
            address = entry.donorAddress,
            modifier = Modifier.size(if (podium) PodiumAvatarSize else ListAvatarSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = truncateAddress(entry.donorAddress),
                color = LivanaColors.Text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "${entry.donationCount} donations",
                modifier = Modifier.padding(top = 2.dp),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            text = entry.totalDonated.formatWhole(),
            color = LivanaColors.Primary,
            style = if (podium) {
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            } else {
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            },
        )
    }
}

@Composable
private fun RankBadge(rank: Int, podium: Boolean) {
    if (podium) {
        val (container, ink) = podiumBadgeColors(rank)
        Box(
            modifier = Modifier
                .size(PodiumBadgeSize)
                .background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rank.toString(),
                color = ink,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
        }
    } else {
        Text(
            text = rank.toString(),
            modifier = Modifier.width(ListRankWidth),
            color = LivanaColors.TextMuted,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
    }
}

private fun podiumBadgeColors(rank: Int): Pair<Color, Color> = when (rank) {
    1 -> LivanaColors.GoldContainer to LivanaColors.PausedInk
    2 -> LivanaColors.SurfaceAlt to LivanaColors.TextSecondary
    else -> LivanaColors.SecondaryContainer to LivanaColors.SecondaryInk
}

private val RowVerticalPadding = 13.dp
private val PodiumAvatarSize = 44.dp
private val ListAvatarSize = 38.dp
private val PodiumBadgeSize = 30.dp
private val ListRankWidth = 20.dp
