package com.livana.app.core.chain.wallet

/**
 * Wallet connection state, kept independent of any WalletConnect/Reown type so the rest of the app
 * never imports the SDK directly.
 */
sealed interface WalletConnectionState {
    data object Disconnected : WalletConnectionState

    /**
     * @param address the connected EVM account (`0x…`).
     * @param chainId the CAIP-2 chain id of the active session (e.g. `eip155:31337`).
     */
    data class Connected(
        val address: String,
        val chainId: String,
    ) : WalletConnectionState
}
