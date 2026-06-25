package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.DonorDonation
import com.livana.app.core.model.DonorLeaderboardEntry
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PoolDonation

interface DonationRepository {
    suspend fun getPoolDonations(
        address: String,
        page: Int? = null,
        size: Int? = null,
    ): LivanaResult<PagedResult<PoolDonation>>

    suspend fun getMyDonations(
        page: Int? = null,
        size: Int? = null,
    ): LivanaResult<PagedResult<DonorDonation>>

    suspend fun getDonorLeaderboard(
        limit: Int? = null,
    ): LivanaResult<List<DonorLeaderboardEntry>>
}
