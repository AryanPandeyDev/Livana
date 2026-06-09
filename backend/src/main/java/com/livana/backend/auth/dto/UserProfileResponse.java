package com.livana.backend.auth.dto;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        String walletAddress,
        String role,
        OffsetDateTime createdAt
) {}
