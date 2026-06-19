package com.livana.app.core.model

data class NgoLeaderboardEntry(
    val ngoAddress: String,
    val orgName: String?,
    val totalSbts: Long,
    val totalAmountReleased: Usdc,
    val poolCount: Long,
    val rank: Int,
)
