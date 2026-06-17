package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PoolSummaryDto(
    val onChainAddress: String,
    val title: String,
    val description: String,
    val region: String,
    val coverImageCid: String?,
    val targetAmount: Long,
    val totalDonated: Long,
    val totalReleased: Long,
    val isPaused: Boolean,
    val deployedAt: String,
)
