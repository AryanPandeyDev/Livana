package com.livana.app.feature.pooldetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaCardStyle
import com.livana.app.core.designsystem.component.LivanaTabs
import com.livana.app.core.designsystem.component.LivanaTextButton
import com.livana.app.core.designsystem.component.ShieldIcon
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.PoolDetail
import com.livana.app.core.model.PoolDonation
import com.livana.app.core.model.Proof
import com.livana.app.feature.pooldetail.CollapsedDescriptionLines
import com.livana.app.feature.pooldetail.PoolDetailTabs
import com.livana.app.feature.pooldetail.PreviewRows
import com.livana.app.core.designsystem.component.DividerLine

@Composable
fun PoolDetailTabs(
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
            ShieldIcon(tint = LivanaColors.Primary)
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
