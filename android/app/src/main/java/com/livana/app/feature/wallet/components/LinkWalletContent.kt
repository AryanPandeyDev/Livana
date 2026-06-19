package com.livana.app.feature.wallet.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livana.app.core.designsystem.component.BackChevronIcon
import com.livana.app.core.designsystem.component.CheckIcon
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.component.LivanaCard
import com.livana.app.core.designsystem.component.LivanaOutlineButton
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.ComponentDimens
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.feature.wallet.LinkWalletUiState

/** Branded "Verify your wallet" frame (13-link-wallet.html) showing link progress + errors. */
@Composable
internal fun LinkWalletContent(
    state: LinkWalletUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        LinkWalletAppBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Spacing.ScreenHorizontal,
                    end = Spacing.ScreenHorizontal,
                    bottom = Spacing.S24,
                ),
        ) {
            Text(
                text = "Sign a quick message to prove this wallet is yours. It's free and not a blockchain transaction.",
                modifier = Modifier.padding(top = Spacing.S8),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )

            messagePreview(state)?.let { message ->
                MessageCard(
                    message = message,
                    modifier = Modifier.padding(top = Spacing.S16),
                )
            }

            when (state) {
                is LinkWalletUiState.Connecting ->
                    StatusRow(text = "Connecting your wallet…", modifier = Modifier.padding(top = Spacing.S20))

                is LinkWalletUiState.AwaitingSignature ->
                    StatusRow(text = "Waiting for signature in your wallet…", modifier = Modifier.padding(top = Spacing.S20))

                is LinkWalletUiState.Verifying ->
                    StatusRow(text = "Verifying signature…", modifier = Modifier.padding(top = Spacing.S20))

                is LinkWalletUiState.Done ->
                    DoneRow(modifier = Modifier.padding(top = Spacing.S20))

                is LinkWalletUiState.Error -> ErrorBlock(
                    message = state.message,
                    canRetry = state.canRetry,
                    onRetry = onRetry,
                    onBack = onBack,
                    modifier = Modifier.padding(top = Spacing.S20),
                )
            }
        }
    }
}

private fun messagePreview(state: LinkWalletUiState): String? = when (state) {
    is LinkWalletUiState.AwaitingSignature -> state.message
    is LinkWalletUiState.Verifying -> state.message
    else -> null
}

@Composable
private fun LinkWalletAppBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.S8, vertical = Spacing.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonLivana(onClick = onBack, contentDescription = "Back") {
            BackChevronIcon()
        }
        Text(
            text = "Verify your wallet",
            modifier = Modifier.weight(1f),
            color = LivanaColors.Text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.width(ComponentDimens.IconChipSize))
    }
}

@Composable
private fun MessageCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    LivanaCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "MESSAGE TO SIGN",
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = Spacing.S12),
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

@Composable
private fun StatusRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(ComponentDimens.SmallIconSize),
            color = LivanaColors.Primary,
            strokeWidth = Borders.Spinner,
        )
        Text(
            text = text,
            color = LivanaColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DoneRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckIcon(tint = LivanaColors.Primary)
        Text(
            text = "Wallet linked.",
            color = LivanaColors.Primary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun ErrorBlock(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.S12),
    ) {
        Text(
            text = message,
            color = LivanaColors.SecondaryInk,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (canRetry) {
            LivanaPrimaryButton(
                text = "Try again",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LivanaOutlineButton(
            text = "Back to profile",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
