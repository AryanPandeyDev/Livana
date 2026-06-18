package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProofDto(
    val proofId: Int,
    val ipfsCid: String,
    val amount: Long,
    val released: Boolean,
    val submittedAt: String,
    val releasedAt: String?,
)
