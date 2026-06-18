package com.livana.app.core.model

data class NgoReputation(
    val ngoAddress: String,
    val orgName: String?,
    val totalSbts: Long,
    val totalAmountReleased: Usdc,
    val poolCount: Long,
)
