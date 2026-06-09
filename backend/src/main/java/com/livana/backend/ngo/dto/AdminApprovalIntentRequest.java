package com.livana.backend.ngo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin request to record approval intent for an NGO application.
 * This records the admin's intent in the backend — the actual on-chain whitelisting
 * happens via the Safe multi-sig UI, and the backend reacts to the NGOApproved event.
 */
public record AdminApprovalIntentRequest(
        String adminNotes
) {}
