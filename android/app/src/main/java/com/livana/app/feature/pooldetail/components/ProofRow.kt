package com.livana.app.feature.pooldetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.livana.app.core.common.toShortDate
import com.livana.app.core.designsystem.component.DocumentIcon
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.StatusPill
import com.livana.app.core.designsystem.component.StatusPillKind
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.Proof

@Composable
fun ProofRow(proof: Proof) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.S12),
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(tint = IconChipTint.Jade) {
            DocumentIcon(tint = LocalContentColor.current)
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
