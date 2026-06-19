package com.livana.app.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.navigation.ModalBottomSheetLayout
import androidx.compose.material.navigation.rememberBottomSheetNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.feature.wallet.components.LinkWalletContent
import com.reown.appkit.ui.appKitGraph
import com.reown.appkit.ui.openAppKit

/**
 * Connect + link flow (12-connect-wallet.html + 13-link-wallet.html).
 *
 * Hosts a private nested NavHost with a [BottomSheetNavigator] so AppKit's connect modal
 * (registered via [appKitGraph]) can be shown over our branded content without touching the main
 * app nav graph. The modal launcher (`openAppKit`) is handed to the ViewModel so the LinkWallet
 * use-case can open it when a session is needed.
 */
@Composable
fun LinkWalletScreen(
    onBack: () -> Unit,
    onLinked: () -> Unit,
    viewModel: LinkWalletViewModel = hiltViewModel(),
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(navController) {
        viewModel.setModalLauncher { navController.openAppKit(shouldOpenChooseNetwork = false) }
        viewModel.start()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.setModalLauncher(null) }
    }
    LaunchedEffect(state) {
        if (state is LinkWalletUiState.Done) onLinked()
    }

    ModalBottomSheetLayout(bottomSheetNavigator = bottomSheetNavigator) {
        NavHost(
            navController = navController,
            startDestination = ContentRoute,
            modifier = Modifier
                .fillMaxSize()
                .background(LivanaColors.Canvas),
        ) {
            composable(ContentRoute) {
                LinkWalletContent(
                    state = state,
                    onBack = onBack,
                    onRetry = viewModel::retry,
                )
            }
            appKitGraph(navController)
        }
    }
}

private const val ContentRoute = "link-wallet-content"
