package com.livana.app.core.network

import com.livana.app.core.network.dto.LeaderboardEntryDto
import com.livana.app.core.network.dto.PoolDonationDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DonationApi {
    @GET("api/v1/donations/pool/{poolAddress}")
    suspend fun getPoolDonations(
        @Path("poolAddress") address: String,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): Response<Page<PoolDonationDto>>

    @GET("api/v1/donations/leaderboard")
    suspend fun getLeaderboard(
        @Query("limit") limit: Int? = null,
    ): Response<List<LeaderboardEntryDto>>
}
