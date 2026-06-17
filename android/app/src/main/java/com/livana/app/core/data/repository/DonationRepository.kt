package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PoolDonation

interface DonationRepository {
    suspend fun getPoolDonations(
        address: String,
        page: Int? = null,
        size: Int? = null,
    ): LivanaResult<PagedResult<PoolDonation>>
}
