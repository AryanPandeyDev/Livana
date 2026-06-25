package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.DonorDonation
import com.livana.app.core.model.DonorLeaderboardEntry
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PoolDonation
import com.livana.app.core.network.DonationApi
import com.livana.app.core.network.mapper.toDomain
import com.livana.app.core.network.toDomainError
import javax.inject.Inject

class DonationRepositoryImpl @Inject constructor(
    private val donationApi: DonationApi,
) : DonationRepository {
    override suspend fun getPoolDonations(
        address: String,
        page: Int?,
        size: Int?,
    ): LivanaResult<PagedResult<PoolDonation>> = try {
        donationApi.getPoolDonations(
            address = address,
            page = page,
            size = size,
        ).toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }

    override suspend fun getMyDonations(
        page: Int?,
        size: Int?,
    ): LivanaResult<PagedResult<DonorDonation>> = try {
        donationApi.getMyDonations(
            page = page,
            size = size,
        ).toLivanaResult { it.toDomain() }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }

    override suspend fun getDonorLeaderboard(
        limit: Int?,
    ): LivanaResult<List<DonorLeaderboardEntry>> = try {
        donationApi.getLeaderboard(limit = limit)
            .toLivanaResult { entries -> entries.map { it.toDomain() } }
    } catch (throwable: Throwable) {
        LivanaResult.Failure(throwable.toDomainError())
    }
}
