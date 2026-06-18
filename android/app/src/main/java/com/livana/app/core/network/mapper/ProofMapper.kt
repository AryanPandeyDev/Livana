package com.livana.app.core.network.mapper

import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.Proof
import com.livana.app.core.network.Page
import com.livana.app.core.network.dto.ProofDto

fun ProofDto.toDomain(): Proof = Proof(
    proofId = proofId,
    ipfsCid = ipfsCid,
    amount = amount.toUsdc(),
    released = released,
    submittedAt = submittedAt,
    releasedAt = releasedAt,
)

@JvmName("proofPageToDomain")
fun Page<ProofDto>.toDomain(): PagedResult<Proof> = PagedResult(
    content = content.map(ProofDto::toDomain),
    totalElements = totalElements,
    totalPages = totalPages,
    first = first,
    last = last,
    number = number,
    size = size,
    numberOfElements = numberOfElements,
    empty = empty,
)
