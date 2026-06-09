package com.livana.backend.pool.dto;

public record PlatformStatsDto(
    long totalDonated,
    long totalReleased,
    long totalPoolsCount,
    long activePoolsCount,
    long verifiedNgosCount
) {}
