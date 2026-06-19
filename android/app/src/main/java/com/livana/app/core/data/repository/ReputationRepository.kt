package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.NgoLeaderboardEntry
import com.livana.app.core.model.NgoReputation
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.SbtMint

interface ReputationRepository {
    suspend fun getReputation(address: String): LivanaResult<NgoReputation>
    suspend fun getReputationHistory(
        address: String,
        page: Int? = null,
        size: Int? = null
    ): LivanaResult<PagedResult<SbtMint>>

    suspend fun getNgoLeaderboard(
        limit: Int? = null
    ): LivanaResult<List<NgoLeaderboardEntry>>
}