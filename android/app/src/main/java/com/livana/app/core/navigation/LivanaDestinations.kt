package com.livana.app.core.navigation

/**
 * Type-safe navigation routes. Bottom-nav shell: Home, Explore, Boards, Activity, Profile.
 * NGO destinations surface inside Activity/Profile once the user is NGO-unlocked.
 *
 * Implement with Navigation-Compose (a NavHost in core/navigation/LivanaNavHost.kt).
 */
sealed interface Destination {
    val route: String

    // Top-level (bottom nav)
    data object Home : Destination { override val route = "home" }
    data object Explore : Destination { override val route = "explore" }
    data object Boards : Destination { override val route = "boards" }
    data object Activity : Destination { override val route = "activity" }
    data object Profile : Destination { override val route = "profile" }

    // Detail / flow destinations (args shown as templates)
    data object PoolDetail : Destination { override val route = "pool/{address}" }
    data object PoolDonations : Destination { override val route = "pool/{address}/donations" }
    data object PoolProofs : Destination { override val route = "pool/{address}/proofs" }
    data object Donate : Destination { override val route = "donate/{address}" }
    data object NgoProfile : Destination { override val route = "ngo/{address}" }
    data object Settings : Destination { override val route = "settings" }
    data object SignIn : Destination { override val route = "sign-in" }
    data object LinkWallet : Destination { override val route = "wallet/link" }
    data object BecomeNgo : Destination { override val route = "ngo/apply" }
    data object CreatePool : Destination { override val route = "ngo/pool/create" }
    data object SubmitProof : Destination { override val route = "ngo/pool/{address}/proof" }
    data object PinataKeys : Destination { override val route = "ngo/pinata" }
}
