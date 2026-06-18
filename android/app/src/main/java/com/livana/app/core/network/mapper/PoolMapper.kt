package com.livana.app.core.network.mapper

import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.NgoReputation
import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.PoolDetail
import com.livana.app.core.model.PoolDonation
import com.livana.app.core.model.Proof
import com.livana.app.core.model.Region
import com.livana.app.core.network.Page
import com.livana.app.core.network.dto.NgoReputationDto
import com.livana.app.core.network.dto.PoolDetailDto
import com.livana.app.core.network.dto.PoolDonationDto
import com.livana.app.core.network.dto.ProofDto
import com.livana.app.core.network.dto.PoolSummaryDto

fun PoolSummaryDto.toDomain(): PoolSummary = PoolSummary(
    onChainAddress = onChainAddress,
    title = title,
    description = description,
    region = Region.fromDisplay(region),
    coverImageCid = coverImageCid,
    targetAmount = targetAmount.toUsdc(),
    totalDonated = totalDonated.toUsdc(),
    totalReleased = totalReleased.toUsdc(),
    isPaused = isPaused,
    deployedAt = deployedAt,
)

@JvmName("poolSummaryPageToDomain")
fun Page<PoolSummaryDto>.toDomain(): PagedResult<PoolSummary> = PagedResult(
    content = content.map(PoolSummaryDto::toDomain),
    totalElements = totalElements,
    totalPages = totalPages,
    first = first,
    last = last,
    number = number,
    size = size,
    numberOfElements = numberOfElements,
    empty = empty,
)

fun PoolDetailDto.toDomain(): PoolDetail = PoolDetail(
    onChainAddress = onChainAddress,
    creatorAddress = creatorAddress,
    poolIndex = poolIndex,
    metadataCid = metadataCid,
    title = title,
    description = description,
    region = Region.fromDisplay(region),
    coverImageCid = coverImageCid,
    targetAmount = targetAmount.toUsdc(),
    totalDonated = totalDonated.toUsdc(),
    totalReleased = totalReleased.toUsdc(),
    isPaused = isPaused,
    deployTxHash = deployTxHash,
    deployBlock = deployBlock,
    deployedAt = deployedAt,
    donationCount = donationCount,
    proofCount = proofCount,
    recentDonations = recentDonations.map(PoolDonationDto::toDomain),
    recentProofs = recentProofs.map(ProofDto::toDomain),
    creatorReputation = creatorReputation.toDomain(),
)


