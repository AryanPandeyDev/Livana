package com.livana.app.core.chain.wallet

import android.app.Application
import com.livana.app.BuildConfig
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal

/**
 * Guarded one-time initialization of Reown AppKit (WalletConnect), called from
 * `LivanaApplication.onCreate`. Mirrors the Clerk init guard: when the WalletConnect Project ID is
 * blank (not yet provided via local.properties) initialization is skipped entirely so the app never
 * crashes — wallet features simply stay unavailable until a Project ID is present.
 */
object ReownAppKitInitializer {

    fun initialize(application: Application) {
        val projectId = BuildConfig.WALLETCONNECT_PROJECT_ID
        if (projectId.isBlank()) return

        val appMetaData = Core.Model.AppMetaData(
            name = "Livana",
            description = "Give with proof, not promises.",
            url = "https://livana.app",
            icons = emptyList(),
            redirect = REDIRECT,
        )

        CoreClient.initialize(
            application = application,
            projectId = projectId,
            metaData = appMetaData,
            connectionType = ConnectionType.AUTOMATIC,
            onError = { /* No-op: wallet features degrade gracefully if relay init fails. */ },
        )

        AppKit.initialize(
            init = Modal.Params.Init(core = CoreClient, coinbaseEnabled = false),
            onSuccess = {
                // Chains must be set before opening the connect modal (Reown usage docs).
                AppKit.setChains(listOf(livanaChain()))
            },
            onError = { /* No-op: surfaced as an unavailable wallet connector at runtime. */ },
        )
    }

    /** Single EVM chain derived from build config (Anvil now, testnet/mainnet later). */
    private fun livanaChain(): Modal.Model.Chain = Modal.Model.Chain(
        chainName = "Livana Chain",
        chainNamespace = "eip155",
        chainReference = BuildConfig.CHAIN_ID.toString(),
        // Both signing and sending must be REQUIRED so the wallet authorizes them for this chain;
        // an optional method the wallet skips causes "method not authorized" on eth_sendTransaction.
        requiredMethods = listOf("personal_sign", "eth_sendTransaction"),
        optionalMethods = listOf("eth_signTypedData_v4", "wallet_switchEthereumChain"),
        events = listOf("chainChanged", "accountsChanged"),
        token = Modal.Model.Token(name = "USD Coin", symbol = "USDC", decimal = 6),
        rpcUrl = BuildConfig.RPC_URL,
    )

    const val REDIRECT = "livana-wc://request"
}
