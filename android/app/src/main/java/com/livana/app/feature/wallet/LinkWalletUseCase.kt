package com.livana.app.feature.wallet

import com.livana.app.core.auth.SessionManager
import com.livana.app.core.chain.wallet.PersonalSignResult
import com.livana.app.core.chain.wallet.WalletConnectionState
import com.livana.app.core.chain.wallet.WalletConnector
import com.livana.app.core.common.BackendErrorCode
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.UserRepository
import javax.inject.Inject

/**
 * Orchestrates wallet linking (the canonical hybrid use-case per PROJECT_STRUCTURE):
 *
 *  1. ensure a WalletConnect session (connect if needed)
 *  2. request a fresh challenge from the backend
 *  3. `personal_sign` the challenge message verbatim (newlines included)
 *  4. PATCH the signed challenge; on success refresh the cached profile via [SessionManager]
 *
 * A stale/missing challenge at the PATCH step (`NO_CHALLENGE` / `CHALLENGE_EXPIRED`) triggers one
 * re-request → re-sign → re-PATCH retry, since a new challenge means a new nonce to sign.
 */
class LinkWalletUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val walletConnector: WalletConnector,
    private val sessionManager: SessionManager,
) {
    suspend operator fun invoke(onPhase: (LinkWalletPhase) -> Unit = {}): LinkWalletResult {
        onPhase(LinkWalletPhase.Connecting)
        val connected = walletConnector.connect() as? WalletConnectionState.Connected
            ?: return LinkWalletResult.NoSession

        val address = connected.address
        if (!ADDRESS_REGEX.matches(address)) {
            return LinkWalletResult.Error("Connected wallet address is invalid")
        }

        var attempt = 0
        while (true) {
            attempt++

            val message = when (val challenge = userRepository.getWalletChallenge()) {
                is LivanaResult.Success -> challenge.value
                is LivanaResult.Failure -> return challenge.error.toLinkFailure()
            }

            onPhase(LinkWalletPhase.AwaitingSignature(message))
            val signature = when (val signed = walletConnector.personalSign(message)) {
                is PersonalSignResult.Success -> signed.signature
                PersonalSignResult.Rejected -> return LinkWalletResult.UserRejected
                PersonalSignResult.NoSession -> return LinkWalletResult.NoSession
                is PersonalSignResult.Failed -> return LinkWalletResult.Error(signed.message)
            }

            if (!SIGNATURE_REGEX.matches(signature)) {
                return LinkWalletResult.Error("Wallet returned an invalid signature")
            }

            onPhase(LinkWalletPhase.Verifying(message))
            when (val linked = userRepository.linkWallet(address, signature, message)) {
                is LivanaResult.Success -> {
                    sessionManager.refresh()
                    return LinkWalletResult.Success(linked.value)
                }

                is LivanaResult.Failure -> {
                    val error = linked.error
                    when ((error as? DomainError.Backend)?.code) {
                        BackendErrorCode.NoChallenge,
                        BackendErrorCode.ChallengeExpired,
                        -> if (attempt < MaxAttempts) continue else return LinkWalletResult.Error(error.message)

                        BackendErrorCode.WalletAlreadyLinked -> return LinkWalletResult.WalletAlreadyLinked
                        BackendErrorCode.SignatureInvalid -> return LinkWalletResult.SignatureInvalid
                        BackendErrorCode.ChallengeMismatch -> return LinkWalletResult.ChallengeMismatch
                        else -> return error.toLinkFailure()
                    }
                }
            }
        }
    }

    private fun DomainError.toLinkFailure(): LinkWalletResult =
        if (this is DomainError.Network) LinkWalletResult.Offline else LinkWalletResult.Error(message)

    private companion object {
        const val MaxAttempts = 2
        val ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
        val SIGNATURE_REGEX = Regex("^0x[a-fA-F0-9]{130}$")
    }
}
