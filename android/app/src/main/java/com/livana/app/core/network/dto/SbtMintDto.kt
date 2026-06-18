package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable


@Serializable
data class SbtMintDto(
    val tokenId : Long,
    val poolAddress: String,
    val amount : Long,
    val txHash : String,
    val blockTimestamp : String
)
