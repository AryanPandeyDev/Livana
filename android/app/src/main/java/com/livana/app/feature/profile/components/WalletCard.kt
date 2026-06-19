package com.livana.app.feature.profile.components

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
import com.livana.app.core.designsystem.component.AddressText
import com.livana.app.core.designsystem.component.DividerLine
import com.livana.app.core.designsystem.component.IconChip
import com.livana.app.core.designsystem.component.IconChipTint
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaOutlineButton
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.component.ShieldIcon
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing

/**
 * Wallet section of the Profile (19-profile.html). Shows the linked address with a re-link action,
 * or a "Link wallet" call-to-action when no wallet is linked yet.
 */
@Composable
internal fun WalletCard(
    walletAddress: String?,
    onLinkWallet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LivanaCard(modifier = modifier.fillMaxWidth()) {
        if (walletAddress == null) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.S12)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconChip(tint = IconChipTint.Jade) {
                        ShieldIcon(tint = LocalContentColor.current)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No wallet linked",
                            color = LivanaColors.Text,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Link a wallet to donate and prove ownership on-chain.",
                            color = LivanaColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                LivanaPrimaryButton(
                    text = "Link wallet",
                    onClick = onLinkWallet,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconChip(tint = IconChipTint.Jade) {
                        ShieldIcon(tint = LocalContentColor.current)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linked wallet",
                            color = LivanaColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        AddressText(address = walletAddress)
                    }
                }
                DividerLine()
                LivanaOutlineButton(
                    text = "Re-link wallet",
                    onClick = onLinkWallet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.S12),
                )
            }
        }
    }
}
