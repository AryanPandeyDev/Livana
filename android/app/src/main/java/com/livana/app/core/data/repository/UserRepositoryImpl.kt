package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.UserProfile
import com.livana.app.core.network.UserApi
import com.livana.app.core.network.dto.LinkWalletRequestDto
import com.livana.app.core.network.mapper.toDomain
import com.livana.app.core.network.toDomainError
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
) : UserRepository {
    override suspend fun getMe(): LivanaResult<UserProfile> = try {
        userApi.getMe().toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }

    override suspend fun getWalletChallenge(): LivanaResult<String> = try {
        userApi.getWalletChallenge().toLivanaResult { it.message }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }

    override suspend fun linkWallet(
        walletAddress: String,
        signature: String,
        message: String,
    ): LivanaResult<UserProfile> = try {
        userApi.linkWallet(
            LinkWalletRequestDto(
                walletAddress = walletAddress,
                signature = signature,
                message = message,
            ),
        ).toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }
}
