package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PoolDonationDto(
    val donorAddress: String,
    val amount: Long,
    val txHash: String,
    val blockTimestamp: String,
)
