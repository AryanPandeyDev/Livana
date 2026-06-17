package com.livana.app.core.model

data class PlatformStats(
    val totalDonated: Usdc,
    val totalReleased: Usdc,
    val totalPoolsCount: Long,
    val activePoolsCount: Long,
    val verifiedNgosCount: Long,
)
