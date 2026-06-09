package com.livana.backend.reputation.dto;

public record NgoLeaderboardEntryDto(
    String ngoAddress,
    long totalSbts,
    long totalAmountReleased,
    long poolCount,
    int rank
) {}
