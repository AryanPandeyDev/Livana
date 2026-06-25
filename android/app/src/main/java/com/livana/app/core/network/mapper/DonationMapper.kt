package com.livana.app.core.network.mapper

import com.livana.app.core.model.DonorDonation
import com.livana.app.core.model.DonorLeaderboardEntry
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PoolDonation
import com.livana.app.core.network.Page
import com.livana.app.core.network.dto.DonorDonationDto
import com.livana.app.core.network.dto.LeaderboardEntryDto
import com.livana.app.core.network.dto.PoolDonationDto

fun PoolDonationDto.toDomain(): PoolDonation = PoolDonation(
    donorAddress = donorAddress,
    amount = amount.toUsdc(),
    txHash = txHash,
    blockTimestamp = blockTimestamp,
)

fun DonorDonationDto.toDomain(): DonorDonation = DonorDonation(
    poolAddress = poolAddress,
    amount = amount.toUsdc(),
    txHash = txHash,
    blockTimestamp = blockTimestamp,
)

fun LeaderboardEntryDto.toDomain(): DonorLeaderboardEntry = DonorLeaderboardEntry(
    donorAddress = donorAddress,
    totalDonated = totalDonated.toUsdc(),
    donationCount = donationCount,
)

@JvmName("poolDonationPageToDomain")
fun Page<PoolDonationDto>.toDomain(): PagedResult<PoolDonation> = PagedResult(
    content = content.map(PoolDonationDto::toDomain),
    totalElements = totalElements,
    totalPages = totalPages,
    first = first,
    last = last,
    number = number,
    size = size,
    numberOfElements = numberOfElements,
    empty = empty,
)

@JvmName("donorDonationPageToDomain")
fun Page<DonorDonationDto>.toDomain(): PagedResult<DonorDonation> = PagedResult(
    content = content.map(DonorDonationDto::toDomain),
    totalElements = totalElements,
    totalPages = totalPages,
    first = first,
    last = last,
    number = number,
    size = size,
    numberOfElements = numberOfElements,
    empty = empty,
)
