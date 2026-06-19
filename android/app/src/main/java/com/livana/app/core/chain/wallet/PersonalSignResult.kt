package com.livana.app.core.chain.wallet

/** Typed outcome of requesting a `personal_sign` from the connected wallet. */
sealed interface PersonalSignResult {
    /** The wallet returned a signature (`0x…`, 130 hex chars for an EIP-191 signature). */
    data class Success(val signature: String) : PersonalSignResult

    /** The user rejected/declined the signature request in their wallet. */
    data object Rejected : PersonalSignResult

    /** No active wallet session, or WalletConnect isn't configured. */
    data object NoSession : PersonalSignResult

    /** Any other failure (transport error, timeout, malformed response). */
    data class Failed(val message: String?) : PersonalSignResult
}
