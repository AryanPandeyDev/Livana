package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PoolDetail
import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.Region
import com.livana.app.core.network.PoolApi
import com.livana.app.core.network.mapper.toDomain
import com.livana.app.core.network.toDomainError
import javax.inject.Inject

class PoolRepositoryImpl @Inject constructor(
    private val poolApi: PoolApi,
) : PoolRepository {
    override suspend fun getPools(
        region: Region?,
        search: String?,
        page: Int?,
        size: Int?,
        sort: String?,
    ): LivanaResult<PagedResult<PoolSummary>> = try {
        poolApi.getPools(
            region = region?.display,
            search = search,
            page = page,
            size = size,
            sort = sort,
        ).toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }

    override suspend fun getPool(address: String): LivanaResult<PoolDetail> = try {
        poolApi.getPool(address = address).toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }
}
