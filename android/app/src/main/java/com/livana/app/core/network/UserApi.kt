package com.livana.app.core.network

import com.livana.app.core.network.dto.LinkWalletRequestDto
import com.livana.app.core.network.dto.UserProfileDto
import com.livana.app.core.network.dto.WalletChallengeDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getMe(): Response<UserProfileDto>

    @GET("api/v1/users/me/wallet/challenge")
    suspend fun getWalletChallenge(): Response<WalletChallengeDto>

    @PATCH("api/v1/users/me/wallet")
    suspend fun linkWallet(@Body body: LinkWalletRequestDto): Response<UserProfileDto>
}
