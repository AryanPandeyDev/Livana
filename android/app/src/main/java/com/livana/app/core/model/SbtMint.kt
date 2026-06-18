package com.livana.app.core.model

data class SbtMint(
    val tokenId: Long,
    val poolAddress: String,
    val amount: Usdc,
    val txHash: String,
    val blockTimestamp: String
)
