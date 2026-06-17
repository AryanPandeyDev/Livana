package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.Proof
import com.livana.app.core.network.ProofApi
import com.livana.app.core.network.mapper.toDomain
import com.livana.app.core.network.toDomainError
import javax.inject.Inject

class ProofRepositoryImpl @Inject constructor(
    private val proofApi: ProofApi,
) : ProofRepository {
    override suspend fun getPoolProofs(
        address: String,
        page: Int?,
        size: Int?,
    ): LivanaResult<PagedResult<Proof>> = try {
        proofApi.getPoolProofs(
            address = address,
            page = page,
            size = size,
        ).toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }
}
