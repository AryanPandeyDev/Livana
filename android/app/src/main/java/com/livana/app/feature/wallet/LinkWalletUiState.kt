package com.livana.app.feature.wallet

sealed interface LinkWalletUiState {
    /** Ensuring a wallet session (the AppKit connect modal may be shown over this screen). */
    data object Connecting : LinkWalletUiState

    /** Awaiting the user to approve the `personal_sign` in their wallet. */
    data class AwaitingSignature(val message: String) : LinkWalletUiState

    /** Submitting the signature to the backend. */
    data class Verifying(val message: String) : LinkWalletUiState

    /** Wallet linked successfully. */
    data object Done : LinkWalletUiState

    /** A terminal (or retryable) error with a user-facing message. */
    data class Error(
        val message: String,
        val canRetry: Boolean,
    ) : LinkWalletUiState
}
