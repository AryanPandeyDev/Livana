package com.livana.app.core.model

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String?,
    val walletAddress: String?,
    val role: Role,
    val createdAt: String,
)
