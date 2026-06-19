package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LinkWalletRequestDto(
    val walletAddress: String,
    val signature: String,
    val message: String,
)
