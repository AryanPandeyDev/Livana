package com.livana.app.core.network

import com.livana.app.core.network.dto.ProofDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProofApi {
    @GET("api/v1/proofs/pool/{poolAddress}")
    suspend fun getPoolProofs(
        @Path("poolAddress") address: String,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): Response<Page<ProofDto>>
}
