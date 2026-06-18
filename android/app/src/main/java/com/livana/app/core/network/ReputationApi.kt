package com.livana.app.core.network

import com.livana.app.core.network.dto.NgoReputationDto
import com.livana.app.core.network.dto.SbtMintDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ReputationApi {
    @GET("api/v1/reputation/{address}")
    suspend fun getReputation(
        @Path("address")
        address: String
    ): Response<NgoReputationDto>

    @GET("api/v1/reputation/{address}/history")
    suspend fun getReputationHistory(
        @Path("address")
        address: String,
        @Query("page")
        page: Int? = null,
        @Query("size")
        size: Int? = null
    ): Response<Page<SbtMintDto>>
}