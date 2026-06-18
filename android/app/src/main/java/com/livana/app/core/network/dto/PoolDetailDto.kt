package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PoolDetailDto(
    val onChainAddress: String,
    val creatorAddress: String,
    val poolIndex: Int,
    val metadataCid: String?,
    val title: String,
    val description: String,
    val region: String,
    val coverImageCid: String?,
    val targetAmount: Long,
    val totalDonated: Long,
    val totalReleased: Long,
    val isPaused: Boolean,
    val deployTxHash: String,
    val deployBlock: Long,
    val deployedAt: String,
    val donationCount: Long,
    val proofCount: Long,
    val recentDonations: List<PoolDonationDto>,
    val recentProofs: List<ProofDto>,
    val creatorReputation: NgoReputationDto,
)

