package com.livana.app.core.network

import com.livana.app.core.network.dto.PlatformStatsDto
import retrofit2.Response
import retrofit2.http.GET

interface StatsApi {
    @GET("api/v1/stats")
    suspend fun getStats(): Response<PlatformStatsDto>
}
