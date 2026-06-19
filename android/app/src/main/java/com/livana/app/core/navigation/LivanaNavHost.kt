package com.livana.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.livana.app.core.designsystem.theme.LivanaColors
import com.livana.app.core.designsystem.theme.LivanaTheme
import com.livana.app.feature.auth.SignInScreen
import com.livana.app.feature.explore.ExploreScreen
import com.livana.app.feature.home.HomeScreen
import com.livana.app.feature.leaderboard.BoardsScreen
import com.livana.app.feature.pooldetail.PoolDetailScreen
import com.livana.app.feature.pooldetail.PoolDonationsScreen
import com.livana.app.feature.pooldetail.PoolProofsScreen
import com.livana.app.feature.profile.ProfileScreen
import com.livana.app.feature.reputation.NgoProfileScreen
import com.livana.app.feature.wallet.LinkWalletScreen

/** Top-level tab routes used to decide bottom-bar visibility. */
private val TopLevelRoutes = LivanaTopLevelTabs.map { it.destination.route }.toSet()

@Composable
fun LivanaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val selectedDestination = LivanaTopLevelTabs.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.destination.route } == true
    }?.destination ?: Destination.Home

    // Hide the bottom bar on non-top-level destinations (e.g. pool detail has its own
    // sticky Donate dock and should not show the bottom nav).
    val isTopLevel = currentDestination?.route in TopLevelRoutes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LivanaColors.Canvas,
        // Scaffold contributes only the bottom-bar height; each screen owns its top/status-bar
        // inset (app-bar screens pad it, full-bleed hero screens bleed under it).
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (isTopLevel) {
                LivanaBottomBar(
                    selectedDestination = selectedDestination,
                    onDestinationSelected = { destination ->
                        navController.navigateToTopLevelDestination(destination)
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .background(LivanaColors.Canvas)
                .padding(innerPadding),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onOpenPool = { address -> navController.navigateToPoolDetail(address) },
                    onOpenNgo = { ngoAddress -> navController.navigateToNgoProfile(ngoAddress) },
                    onSeeAllNgos = { navController.navigateToTopLevelDestination(Destination.Boards) },
                    onSignIn = { navController.navigateToSignIn() },
                )
            }
            composable(Destination.Explore.route) {
                ExploreScreen(
                    onOpenPool = { address -> navController.navigateToPoolDetail(address) },
                )
            }
            composable(Destination.Boards.route) {
                BoardsScreen(
                    onOpenNgo = { ngoAddress -> navController.navigateToNgoProfile(ngoAddress) },
                )
            }
            composable(Destination.Activity.route) { LivanaTabPlaceholder("Activity") }
            composable(Destination.Profile.route) {
                ProfileScreen(
                    onSignIn = { navController.navigateToSignIn() },
                    onLinkWallet = { navController.navigateToLinkWallet() },
                )
            }
            composable(
                route = Destination.PoolDetail.route,
                arguments = listOf(
                    navArgument("address") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val address = backStackEntry.arguments?.getString("address").orEmpty()
                PoolDetailScreen(
                    onBack = { navController.popBackStack() },
                    onDonate = { /* TODO: wire Donate once the donate screen exists. */ },
                    onOpenNgo = { ngoAddress -> navController.navigateToNgoProfile(ngoAddress) },
                    onSeeAllDonations = { navController.navigateToPoolDonations(address) },
                    onSeeAllProofs = { navController.navigateToPoolProofs(address) },
                )
            }
            composable(
                route = Destination.PoolDonations.route,
                arguments = listOf(
                    navArgument("address") { type = NavType.StringType },
                ),
            ) {
                PoolDonationsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Destination.PoolProofs.route,
                arguments = listOf(
                    navArgument("address") { type = NavType.StringType },
                ),
            ) {
                PoolProofsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Destination.NgoProfile.route,
                arguments = listOf(
                    navArgument("address") { type = NavType.StringType },
                ),
            ) {
                NgoProfileScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.SignIn.route) {
                SignInScreen(
                    onBack = { navController.popBackStack() },
                    onSignedIn = { navController.popBackStack() },
                )
            }
            composable(Destination.LinkWallet.route) {
                LinkWalletScreen(
                    onBack = { navController.popBackStack() },
                    onLinked = { navController.popBackStack() },
                )
            }
        }
    }
}

/**
 * Navigate to the pool detail screen for the given on-chain [address].
 * Builds the route by replacing the {address} placeholder in [Destination.PoolDetail.route].
 */
private fun NavHostController.navigateToPoolDetail(address: String) {
    navigate(Destination.PoolDetail.route.replace("{address}", address))
}

private fun NavHostController.navigateToPoolDonations(address: String) {
    navigate(Destination.PoolDonations.route.replace("{address}", address))
}

private fun NavHostController.navigateToPoolProofs(address: String) {
    navigate(Destination.PoolProofs.route.replace("{address}", address))
}

private fun NavHostController.navigateToNgoProfile(address: String) {
    navigate(Destination.NgoProfile.route.replace("{address}", address))
}

private fun NavHostController.navigateToSignIn() {
    navigate(Destination.SignIn.route)
}

private fun NavHostController.navigateToLinkWallet() {
    navigate(Destination.LinkWallet.route)
}

private fun NavHostController.navigateToTopLevelDestination(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun LivanaTabPlaceholder(tabName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tabName,
            color = LivanaColors.Text,
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Preview(name = "Livana - Navigation shell", widthDp = 390, heightDp = 844)
@Composable
private fun LivanaNavHostPreview() {
    LivanaTheme {
        LivanaNavHost()
    }
}

