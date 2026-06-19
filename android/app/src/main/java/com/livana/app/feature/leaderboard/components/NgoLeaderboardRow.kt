package com.livana.app.feature.leaderboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.CheckIcon
import com.livana.app.core.designsystem.component.ChevronRightIcon
import com.livana.app.core.designsystem.component.truncateAddress
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.NgoLeaderboardEntry

/**
 * One ranked NGO row, faithful to 10-ngo-leaderboard.html. Shows the org name (or the
 * truncated address when unverified/null) with a jade verified check, a metrics line, and
 * a trailing chevron. Tapping the row opens the NGO profile via [onClick].
 */
@Composable
fun NgoLeaderboardRow(
    entry: NgoLeaderboardEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = RowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.rank.toString(),
            modifier = Modifier.width(RankWidth),
            color = if (entry.rank == 1) LivanaColors.Gold else LivanaColors.TextMuted,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
        )
        AddressAvatar(
            address = entry.ngoAddress,
            modifier = Modifier.size(AvatarSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.orgName ?: truncateAddress(entry.ngoAddress),
                    color = LivanaColors.Text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
                CheckIcon(tint = LivanaColors.Primary)
            }
            Text(
                text = metricsLine(entry),
                modifier = Modifier.padding(top = 2.dp),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        ChevronRightIcon(tint = LivanaColors.TextMuted)
    }
}

private fun metricsLine(entry: NgoLeaderboardEntry) = buildAnnotatedString {
    val sbtLabel = if (entry.totalSbts == 1L) "SBT" else "SBTs"
    val poolLabel = if (entry.poolCount == 1L) "pool" else "pools"
    append("${entry.totalSbts} $sbtLabel · ")
    withStyle(SpanStyle(color = LivanaColors.Primary, fontWeight = FontWeight.Bold)) {
        append("${entry.totalAmountReleased.formatWhole()} released")
    }
    append(" · ${entry.poolCount} $poolLabel")
}

private val RowVerticalPadding = 14.dp
private val AvatarSize = 42.dp
private val RankWidth = 20.dp
