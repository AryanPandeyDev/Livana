package com.livana.app.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livana.app.core.designsystem.component.AddressAvatar
import com.livana.app.core.designsystem.theme.ComponentTextStyles
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.PillShape
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.Role
import com.livana.app.core.model.UserProfile

/** Centered profile header (19-profile.html): avatar, name, email, role pill. */
@Composable
internal fun ProfileHeader(
    user: UserProfile,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar is the derived gradient identicon — wallet address when linked, else a
        // deterministic fallback from the user id (a generic, non-placeholder avatar).
        AddressAvatar(
            address = user.walletAddress ?: user.id,
            modifier = Modifier.size(AvatarSize),
        )
        Text(
            text = user.displayName ?: user.email,
            modifier = Modifier.padding(top = Spacing.S12),
            color = LivanaColors.Text,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = user.email,
            modifier = Modifier.padding(top = 2.dp),
            color = LivanaColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        RolePill(
            role = user.role,
            modifier = Modifier.padding(top = Spacing.S8),
        )
    }
}

@Composable
private fun RolePill(
    role: Role,
    modifier: Modifier = Modifier,
) {
    Text(
        text = role.displayLabel(),
        modifier = modifier
            .background(LivanaColors.SurfaceAlt, PillShape)
            .padding(horizontal = Spacing.S12, vertical = 5.dp),
        color = LivanaColors.TextSecondary,
        style = ComponentTextStyles.Pill.copy(fontWeight = FontWeight.Bold),
    )
}

private fun Role.displayLabel(): String = when (this) {
    Role.USER -> "Donor"
    Role.NGO -> "NGO"
    Role.ADMIN -> "Admin"
}

private val AvatarSize = 66.dp
