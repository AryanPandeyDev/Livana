package com.livana.app.core.model

data class PoolDetail(
    val onChainAddress: String,
    val creatorAddress: String,
    val poolIndex: Int,
    val metadataCid: String?,
    val title: String,
    val description: String,
    val region: Region?,
    val coverImageCid: String?,
    val targetAmount: Usdc,
    val totalDonated: Usdc,
    val totalReleased: Usdc,
    val isPaused: Boolean,
    val deployTxHash: String,
    val deployBlock: Long,
    val deployedAt: String,
    val donationCount: Long,
    val proofCount: Long,
    val recentDonations: List<PoolDonation>,
    val recentProofs: List<Proof>,
    val creatorReputation: NgoReputation,
)

