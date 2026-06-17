package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PoolDetail
import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.Region

interface PoolRepository {
    suspend fun getPools(
        region: Region? = null,
        search: String? = null,
        page: Int? = null,
        size: Int? = null,
        sort: String? = null,
    ): LivanaResult<PagedResult<PoolSummary>>

    suspend fun getPool(address: String): LivanaResult<PoolDetail>
}
