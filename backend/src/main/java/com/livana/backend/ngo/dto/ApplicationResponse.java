package com.livana.backend.ngo.dto;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * NGO-facing application response.
 * Does NOT expose AI screening results or admin notes — those are admin-only.
 */
@Builder
public record ApplicationResponse(
        UUID id,
        String orgName,
        String registrationNumber,
        String description,
        String officialEmail,
        String documentsCid,
        String walletAddress,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
