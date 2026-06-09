package com.livana.backend.ngo.entity;

/**
 * Application status machine:
 * DRAFT → AI_SCREENING → PENDING_REVIEW → APPROVED / REJECTED
 *
 * Email verification is handled by Clerk (the NGO's officialEmail must be
 * a verified email on their Clerk account). The backend does not own OTP
 * generation or email verification.
 *
 * Terminal states: APPROVED, REJECTED.
 * The DB enforces only one active (non-terminal) application per user via
 * a partial unique index: uq_ngo_applications_active_user.
 */
public enum ApplicationStatus {
    DRAFT,
    AI_SCREENING,
    PENDING_REVIEW,
    APPROVED,
    REJECTED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }
}
