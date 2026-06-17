package com.livana.app.core.model

data class PoolSummary(
    val onChainAddress: String,
    val title: String,
    val description: String,
    val region: Region?,
    val coverImageCid: String?,
    val targetAmount: Usdc,
    val totalDonated: Usdc,
    val totalReleased: Usdc,
    val isPaused: Boolean,
    val deployedAt: String,
)
