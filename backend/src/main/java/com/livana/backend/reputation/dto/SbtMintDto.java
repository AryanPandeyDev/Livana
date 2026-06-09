package com.livana.backend.reputation.dto;

import java.time.OffsetDateTime;

/**
 * Individual SBT mint record for NGO history view.
 * PRD: "As an NGO operator, I want to see my own SBT history
 * and cumulative reputation, so that I can showcase my track record."
 */
public record SbtMintDto(
    long tokenId,
    String poolAddress,
    long amount,
    String txHash,
    OffsetDateTime blockTimestamp
) {}
