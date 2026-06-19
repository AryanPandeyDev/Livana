package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.UserProfile

interface UserRepository {
    suspend fun getMe(): LivanaResult<UserProfile>

    /** Request a wallet-ownership challenge message to sign. Returns the verbatim message. */
    suspend fun getWalletChallenge(): LivanaResult<String>

    /** Submit the signed challenge to link [walletAddress]; returns the updated profile. */
    suspend fun linkWallet(
        walletAddress: String,
        signature: String,
        message: String,
    ): LivanaResult<UserProfile>
}
