package com.livana.backend.ngo.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin-facing application response.
 * Includes everything the NGO sees plus AI screening results, admin fields,
 * and the applicant's Clerk login email (userEmail) alongside the claimed
 * officialEmail so admins can see both.
 */
@Builder
public record AdminApplicationResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String orgName,
        String registrationNumber,
        String description,
        String officialEmail,
        String documentsCid,
        String walletAddress,
        String status,
        BigDecimal aiConfidenceScore,
        String aiResearchSummary,
        String aiVerdict,
        String adminNotes,
        String rejectionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
