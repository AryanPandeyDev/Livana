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

@Serializable
data class PoolDonationDto(
    val donorAddress: String,
    val amount: Long,
    val txHash: String,
    val blockTimestamp: String,
)

@Serializable
data class ProofDto(
    val proofId: Int,
    val ipfsCid: String,
    val amount: Long,
    val released: Boolean,
    val submittedAt: String,
    val releasedAt: String?,
)

@Serializable
data class NgoReputationDto(
    val ngoAddress: String,
    val totalSbts: Long,
    val totalAmountReleased: Long,
    val poolCount: Long,
)
