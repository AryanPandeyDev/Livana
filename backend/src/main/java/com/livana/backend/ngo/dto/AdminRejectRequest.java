package com.livana.backend.ngo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin request to reject an NGO application.
 */
public record AdminRejectRequest(
        @NotBlank(message = "Rejection reason is required")
        String rejectionReason,

        String adminNotes
) {}
