package com.livana.app.core.model

data class Proof(
    val proofId: Int,
    val ipfsCid: String,
    val amount: Usdc,
    val released: Boolean,
    val submittedAt: String,
    val releasedAt: String?,
)
