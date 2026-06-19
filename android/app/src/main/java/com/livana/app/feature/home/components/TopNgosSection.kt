package com.livana.app.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.CheckIcon
import com.livana.app.core.designsystem.component.DividerLine
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaTextButton
import com.livana.app.core.designsystem.component.truncateAddress
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.NgoLeaderboardEntry

/**
 * "Top NGOs" section on Home (02-home.html): section header + "See all", then up to 3
 * compact NGO rows grouped in one card. Each row taps through to the NGO profile. The
 * section is only rendered when there are entries — the caller hides it when empty.
 */
@Composable
internal fun TopNgosSection(
    ngos: List<NgoLeaderboardEntry>,
    onOpenNgo: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.ScreenHorizontal,
                    top = Spacing.S24,
                    end = Spacing.ScreenHorizontal,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeSectionTitle(text = "Top NGOs")
            LivanaTextButton(text = "See all", onClick = onSeeAll)
        }

        LivanaCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.ScreenHorizontal, end = Spacing.ScreenHorizontal, top = Spacing.S12),
            contentPadding = PaddingValues(horizontal = Spacing.S16, vertical = Spacing.S4),
        ) {
            Column {
                ngos.forEachIndexed { index, ngo ->
                    TopNgoRow(
                        ngo = ngo,
                        onClick = { onOpenNgo(ngo.ngoAddress) },
                    )
                    if (index < ngos.lastIndex) {
                        DividerLine()
                    }
                }
            }
        }
    }
}

@Composable
private fun TopNgoRow(
    ngo: NgoLeaderboardEntry,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = RowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AddressAvatar(
            address = ngo.ngoAddress,
            modifier = Modifier.size(AvatarSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ngo.orgName ?: truncateAddress(ngo.ngoAddress),
                    color = LivanaColors.Text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
                CheckIcon(tint = LivanaColors.Primary)
            }
            val sbtLabel = if (ngo.totalSbts == 1L) "SBT" else "SBTs"
            Text(
                text = "${ngo.totalSbts} $sbtLabel · ${ngo.totalAmountReleased.formatWhole()} released",
                modifier = Modifier.padding(top = 2.dp),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            text = "#${ngo.rank}",
            color = if (ngo.rank == 1) LivanaColors.Gold else LivanaColors.TextMuted,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

private val RowVerticalPadding = 13.dp
private val AvatarSize = 42.dp
