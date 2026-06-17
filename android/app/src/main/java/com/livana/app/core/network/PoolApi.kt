package com.livana.app.core.network

import com.livana.app.core.network.dto.PoolSummaryDto
import com.livana.app.core.network.dto.PoolDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PoolApi {
    @GET("api/v1/pools")
    suspend fun getPools(
        @Query("region") region: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: String? = null,
    ): Response<Page<PoolSummaryDto>>

    @GET("api/v1/pools/{address}")
    suspend fun getPool(
        @Path("address") address: String,
    ): Response<PoolDetailDto>
}
