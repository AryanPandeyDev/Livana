package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class NgoLeaderboardEntryDto(
    val ngoAddress: String,
    val orgName: String? = null,
    val totalSbts: Long,
    val totalAmountReleased: Long,
    val poolCount: Long,
    val rank: Int,
)
