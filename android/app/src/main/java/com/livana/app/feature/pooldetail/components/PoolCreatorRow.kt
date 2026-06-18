package com.livana.app.feature.pooldetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.AddressText
import com.livana.app.core.designsystem.component.CheckIcon
import com.livana.app.core.designsystem.component.ChevronRightIcon
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolDetail

@Composable
fun PoolCreatorRow(
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
                val orgName = pool.creatorReputation.orgName
                if (orgName != null) {
                    Text(
                        text = orgName,
                        modifier = Modifier.weight(1f, fill = false),
                        color = LivanaColors.Text,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                } else {
                    AddressText(
                        address = pool.creatorAddress,
                        modifier = Modifier.weight(1f, fill = false),
                        showCopyIcon = false,
                    )
                }
                CheckIcon(tint = LivanaColors.Primary)
            }
            Text(
                text = "${pool.creatorReputation.totalSbts} SBTs - ${pool.creatorReputation.totalAmountReleased.formatWhole()} released",
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        ChevronRightIcon(tint = LivanaColors.TextMuted)
    }
}
