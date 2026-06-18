package com.livana.app.core.network.mapper

import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PoolDonation
import com.livana.app.core.network.Page
import com.livana.app.core.network.dto.PoolDonationDto

fun PoolDonationDto.toDomain(): PoolDonation = PoolDonation(
    donorAddress = donorAddress,
    amount = amount.toUsdc(),
    txHash = txHash,
    blockTimestamp = blockTimestamp,
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
