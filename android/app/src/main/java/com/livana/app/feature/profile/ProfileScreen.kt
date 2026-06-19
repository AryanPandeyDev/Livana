package com.livana.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livana.app.core.auth.AuthState
import com.livana.app.core.designsystem.component.LivanaOutlineButton
import com.livana.app.core.designsystem.component.LivanaPrimaryButton
import com.livana.app.core.designsystem.theme.Borders
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.Spacing
import com.livana.app.core.model.Role
import com.livana.app.core.model.UserProfile
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.feature.profile.components.ProfileHeader
import com.livana.app.feature.profile.components.WalletCard

@Composable
fun ProfileScreen(
    onSignIn: () -> Unit,
    onLinkWallet: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    ProfileScreen(
        authState = authState,
        onSignIn = onSignIn,
        onLinkWallet = onLinkWallet,
        onSignOut = viewModel::signOut,
    )
}

@Composable
internal fun ProfileScreen(
    authState: AuthState,
    onSignIn: () -> Unit = {},
    onLinkWallet: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LivanaColors.Canvas)
            .statusBarsPadding(),
    ) {
        ProfileTopBar()
        when (authState) {
            is AuthState.Loading -> LoadingBox()
            is AuthState.SignedOut -> SignedOutContent(onSignIn = onSignIn)
            is AuthState.SignedIn -> SignedInContent(
                user = authState.user,
                onLinkWallet = onLinkWallet,
                onSignOut = onSignOut,
            )
        }
    }
}

@Composable
private fun ProfileTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenHorizontal, vertical = Spacing.S12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.S8),
    ) {
        Box(
            modifier = Modifier
                .width(AccentRuleWidth)
                .height(AccentRuleHeight)
                .background(LivanaColors.Primary),
        )
        Text(
            text = "Profile",
            color = LivanaColors.Text,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun SignedInContent(
    user: UserProfile,
    onLinkWallet: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.ScreenHorizontal,
                end = Spacing.ScreenHorizontal,
                top = Spacing.S8,
                bottom = Spacing.S24,
            ),
    ) {
        ProfileHeader(user = user)
        SectionLabel(text = "Wallet", modifier = Modifier.padding(top = Spacing.S24, bottom = Spacing.S8))
        WalletCard(walletAddress = user.walletAddress, onLinkWallet = onLinkWallet)
        LivanaOutlineButton(
            text = "Sign out",
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.S24),
        )
    }
}

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Sign in to Livana",
            color = LivanaColors.Text,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Sign in to link a wallet, donate, and track your giving.",
            modifier = Modifier.padding(top = Spacing.S8),
            color = LivanaColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        LivanaPrimaryButton(
            text = "Sign in",
            onClick = onSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.S24),
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = LivanaColors.TextSecondary,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.ScreenHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = LivanaColors.Primary,
            strokeWidth = Borders.Spinner,
        )
    }
}

private val AccentRuleHeight = 20.dp
private val AccentRuleWidth = 4.dp

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun ProfileSignedInLinkedPreview() {
    LivanaTheme {
        ProfileScreen(
            authState = AuthState.SignedIn(
                UserProfile(
                    id = "u_1",
                    email = "alice@example.com",
                    displayName = "Alice",
                    walletAddress = "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc",
                    role = Role.USER,
                    createdAt = "2026-06-07T10:00:00.000+00:00",
                ),
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun ProfileSignedInNoWalletPreview() {
    LivanaTheme {
        ProfileScreen(
            authState = AuthState.SignedIn(
                UserProfile(
                    id = "u_2",
                    email = "bob@example.com",
                    displayName = null,
                    walletAddress = null,
                    role = Role.NGO,
                    createdAt = "2026-06-07T10:00:00.000+00:00",
                ),
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EF)
@Composable
private fun ProfileSignedOutPreview() {
    LivanaTheme {
        ProfileScreen(authState = AuthState.SignedOut)
    }
}
