package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.Proof

interface ProofRepository {
    suspend fun getPoolProofs(
        address: String,
        page: Int? = null,
        size: Int? = null,
    ): LivanaResult<PagedResult<Proof>>
}
