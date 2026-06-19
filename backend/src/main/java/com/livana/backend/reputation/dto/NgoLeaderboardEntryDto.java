package com.livana.backend.reputation.dto;

public record NgoLeaderboardEntryDto(
    String ngoAddress,
    /** Public display name of the verified NGO; null if not resolvable. */
    String orgName,
    long totalSbts,
    long totalAmountReleased,
    long poolCount,
    int rank
) {}
