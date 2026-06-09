package com.livana.backend.proof.dto;

import java.time.OffsetDateTime;

public record PendingProofDto(
    String poolAddress,
    int proofId,
    String ipfsCid,
    long amount,
    OffsetDateTime submittedAt
) {}
