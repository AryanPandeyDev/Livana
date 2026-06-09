package com.livana.backend.proof.dto;

import java.time.OffsetDateTime;

/**
 * A proof submission as seen by the NGO that owns the pool.
 *
 * Unlike {@link ProofDto} (scoped to a single known pool), this view spans all
 * of the NGO's pools, so it includes {@code poolAddress} to identify which pool
 * each proof belongs to. Includes full release status so the NGO can see which
 * claims have been paid out (PRD User Story 27).
 */
public record NgoProofDto(
    String poolAddress,
    int proofId,
    String ipfsCid,
    long amount,
    boolean released,
    OffsetDateTime submittedAt,
    OffsetDateTime releasedAt
) {}
