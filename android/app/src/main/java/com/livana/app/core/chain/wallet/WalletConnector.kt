package com.livana.app.core.chain.wallet

import kotlinx.coroutines.flow.StateFlow

/**
 * App-facing wallet abstraction over Reown AppKit (WalletConnect). All AppKit/WalletConnect types
 * stay behind this interface so the rest of the app depends only on these plain models.
 *
 * The connect modal (wallet picker) is a Compose-navigation destination owned by AppKit, so the UI
 * layer supplies the actual modal launcher via [setModalLauncher]; [connect] invokes it and then
 * suspends until a session is established.
 */
interface WalletConnector {
    /** Current connection state; emits [WalletConnectionState.Connected] once a session settles. */
    val connectionState: StateFlow<WalletConnectionState>

    /** Provide (or clear with `null`) the callback that opens AppKit's connect modal. */
    fun setModalLauncher(launcher: (() -> Unit)?)

    /**
     * Ensure a wallet session exists. Returns immediately if already connected; otherwise opens the
     * connect modal (via the launcher) and suspends until connected or it times out.
     */
    suspend fun connect(): WalletConnectionState

    /** Disconnect the active session, if any. */
    suspend fun disconnect()

    /** Request an EIP-191 `personal_sign` of [message] (sent verbatim) from the connected wallet. */
    suspend fun personalSign(message: String): PersonalSignResult

    /**
     * Submit an `eth_sendTransaction` from the connected account to [toAddress] with calldata
     * [data] and (hex-wei) [value]. Returns the broadcast tx hash or a typed failure.
     */
    suspend fun sendTransaction(
        toAddress: String,
        data: String,
        value: String = "0x0",
    ): TxResult

    /** Forward a redirect deep link (wallet returning to the app) to the SDK. */
    fun handleDeepLink(url: String)
}
