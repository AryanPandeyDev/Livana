package com.livana.app.core.model

data class DonorLeaderboardEntry(
    val donorAddress: String,
    val totalDonated: Usdc,
    val donationCount: Long,
)
