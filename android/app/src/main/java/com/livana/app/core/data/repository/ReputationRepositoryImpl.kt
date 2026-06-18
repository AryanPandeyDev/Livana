package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.NgoReputation
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.SbtMint
import com.livana.app.core.network.ReputationApi
import com.livana.app.core.network.mapper.toDomain
import com.livana.app.core.network.toDomainError
import javax.inject.Inject


class ReputationRepositoryImpl @Inject constructor(
    private val reputationApi: ReputationApi
) : ReputationRepository {

    override suspend fun getReputation(address: String): LivanaResult<NgoReputation> = try {
        reputationApi.getReputation(address).toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }

    override suspend fun getReputationHistory(
        address: String,
        page: Int?,
        size: Int?
    ): LivanaResult<PagedResult<SbtMint>> = try {
        reputationApi.getReputationHistory(address,page,size).toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }

}