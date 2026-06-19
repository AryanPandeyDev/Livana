package com.livana.app.core.network.mapper

import com.livana.app.core.model.Role
import com.livana.app.core.model.UserProfile
import com.livana.app.core.network.dto.UserProfileDto

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    email = email,
    displayName = displayName,
    walletAddress = walletAddress,
    role = Role.fromApi(role),
    createdAt = createdAt,
)
