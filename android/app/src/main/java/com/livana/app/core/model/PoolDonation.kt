package com.livana.app.core.model

data class PoolDonation(
    val donorAddress: String,
    val amount: Usdc,
    val txHash: String,
    val blockTimestamp: String,
)
