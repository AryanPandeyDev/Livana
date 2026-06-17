package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.PlatformStats
import com.livana.app.core.network.StatsApi
import com.livana.app.core.network.mapper.toDomain
import com.livana.app.core.network.toDomainError
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val statsApi: StatsApi,
) : StatsRepository {
    override suspend fun getPlatformStats(): LivanaResult<PlatformStats> = try {
        statsApi.getStats().toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }
}
