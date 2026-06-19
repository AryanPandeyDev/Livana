package com.livana.app.feature.wallet

import com.livana.app.core.model.UserProfile

/** Typed outcome of the wallet-linking orchestration, mapped to clear UI messages. */
sealed interface LinkWalletResult {
    data class Success(val user: UserProfile) : LinkWalletResult

    /** The user rejected the signature request in their wallet. */
    data object UserRejected : LinkWalletResult

    /** No wallet session could be established (connect cancelled/timed out). */
    data object NoSession : LinkWalletResult

    /** The wallet is already linked to a different Livana account (`WALLET_ALREADY_LINKED`). */
    data object WalletAlreadyLinked : LinkWalletResult

    /** Recovered signer didn't match the claimed address (`SIGNATURE_INVALID`). */
    data object SignatureInvalid : LinkWalletResult

    /** Submitted message didn't match the issued challenge (`CHALLENGE_MISMATCH`). */
    data object ChallengeMismatch : LinkWalletResult

    /** Network unavailable. */
    data object Offline : LinkWalletResult

    /** Any other failure. */
    data class Error(val message: String?) : LinkWalletResult
}
