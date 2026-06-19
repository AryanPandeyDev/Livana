package com.livana.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.clerk.ui.auth.AuthView
import com.livana.app.core.designsystem.component.BackChevronIcon
import com.livana.app.core.designsystem.component.BrandMark
import com.livana.app.core.designsystem.component.IconButtonLivana
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing

/**
 * Branded wrapper around Clerk's prebuilt [AuthView]. The frame (back chevron, brand mark,
 * "Welcome to Livana" + subhead) is Livana's; the auth form itself is Clerk's prebuilt UI, whose
 * available methods (email/password, OTP, OAuth, …) are driven by the Clerk Dashboard settings.
 */
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.S8, vertical = Spacing.S8),
        ) {
            IconButtonLivana(
                onClick = onBack,
                contentDescription = "Back",
            ) {
                BackChevronIcon()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.S8),
        ) {
            BrandMark()
            Text(
                text = "Welcome to Livana",
                modifier = Modifier.padding(top = Spacing.S8),
                color = LivanaColors.Text,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                text = "Sign in to give with proof — your donations, tracked on-chain to verified impact.",
                color = LivanaColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        AuthView(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Spacing.S16),
            isDismissible = false,
            onAuthComplete = {
                viewModel.onAuthComplete()
                onSignedIn()
            },
        )
    }
}
