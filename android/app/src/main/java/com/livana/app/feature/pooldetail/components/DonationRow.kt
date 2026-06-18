package com.livana.app.feature.pooldetail.components

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
import androidx.compose.ui.text.font.FontWeight
import com.livana.app.core.common.toRelativeTime
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.component.AddressText
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolDonation

@Composable
fun DonationRow(donation: PoolDonation) {
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
