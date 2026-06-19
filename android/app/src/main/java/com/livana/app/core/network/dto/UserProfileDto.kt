package com.livana.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    // `displayName` may be absent for users without one; backend includes nulls (api ref §14).
    val displayName: String? = null,
    // `null` when no wallet is linked yet (api ref §4).
    val walletAddress: String? = null,
    val role: String,
    val createdAt: String,
)
