package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlatformStatsDto(
    val totalDonated: Long,
    val totalReleased: Long,
    val totalPoolsCount: Long,
    val activePoolsCount: Long,
    val verifiedNgosCount: Long,
)
