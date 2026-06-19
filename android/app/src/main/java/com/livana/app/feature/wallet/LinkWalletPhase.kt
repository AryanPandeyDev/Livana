package com.livana.app.feature.wallet

/** Progress phases reported by [LinkWalletUseCase] so the UI can show connecting → signing → verifying. */
sealed interface LinkWalletPhase {
    /** Ensuring a wallet session (connect modal may be shown). */
    data object Connecting : LinkWalletPhase

    /** Challenge fetched; awaiting the user's `personal_sign` in their wallet. */
    data class AwaitingSignature(val message: String) : LinkWalletPhase

    /** Signature obtained; submitting it to the backend for verification. */
    data class Verifying(val message: String) : LinkWalletPhase
}
