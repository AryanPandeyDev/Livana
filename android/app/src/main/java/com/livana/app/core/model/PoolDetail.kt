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

data class PoolDonation(
    val donorAddress: String,
    val amount: Usdc,
    val txHash: String,
    val blockTimestamp: String,
)

data class Proof(
    val proofId: Int,
    val ipfsCid: String,
    val amount: Usdc,
    val released: Boolean,
    val submittedAt: String,
    val releasedAt: String?,
)

data class NgoReputation(
    val ngoAddress: String,
    val totalSbts: Long,
    val totalAmountReleased: Usdc,
    val poolCount: Long,
)
