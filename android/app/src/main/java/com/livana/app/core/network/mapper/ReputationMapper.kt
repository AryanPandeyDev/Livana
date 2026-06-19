package com.livana.app.core.network.mapper

import com.livana.app.core.model.NgoLeaderboardEntry
import com.livana.app.core.model.NgoReputation
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.SbtMint
import com.livana.app.core.network.Page
import com.livana.app.core.network.dto.NgoLeaderboardEntryDto
import com.livana.app.core.network.dto.NgoReputationDto
import com.livana.app.core.network.dto.SbtMintDto

fun NgoReputationDto.toDomain(): NgoReputation = NgoReputation(
    ngoAddress = ngoAddress,
    orgName = orgName,
    totalSbts = totalSbts,
    totalAmountReleased = totalAmountReleased.toUsdc(),
    poolCount = poolCount,
)

fun NgoLeaderboardEntryDto.toDomain(): NgoLeaderboardEntry = NgoLeaderboardEntry(
    ngoAddress = ngoAddress,
    orgName = orgName,
    totalSbts = totalSbts,
    totalAmountReleased = totalAmountReleased.toUsdc(),
    poolCount = poolCount,
    rank = rank,
)

fun SbtMintDto.toDomain(): SbtMint = SbtMint(
    tokenId = tokenId,
    poolAddress = poolAddress,
    amount = amount.toUsdc(),
    txHash = txHash,
    blockTimestamp = blockTimestamp
)

@JvmName("sbtMintToDomain")
fun Page<SbtMintDto>.toDomain(): PagedResult<SbtMint> = PagedResult(
    content = content.map(SbtMintDto::toDomain),
    totalElements = totalElements,
    totalPages = totalPages,
    first = first,
    last = last,
    number = number,
    size = size,
    numberOfElements = numberOfElements,
    empty = empty
)