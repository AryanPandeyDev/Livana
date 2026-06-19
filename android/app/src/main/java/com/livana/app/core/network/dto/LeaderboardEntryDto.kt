package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntryDto(
    val donorAddress: String,
    val totalDonated: Long,
    val donationCount: Long,
)
