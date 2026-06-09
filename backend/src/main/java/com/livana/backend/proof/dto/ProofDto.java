package com.livana.backend.proof.dto;

import java.time.OffsetDateTime;

public record ProofDto(
    int proofId,
    String ipfsCid,
    long amount,
    boolean released,
    OffsetDateTime submittedAt,
    OffsetDateTime releasedAt
) {}
