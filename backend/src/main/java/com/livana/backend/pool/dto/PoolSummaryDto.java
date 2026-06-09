package com.livana.backend.pool.dto;

import java.time.OffsetDateTime;

public record PoolSummaryDto(
    String onChainAddress,
    String title,
    String description,
    String region,
    String coverImageCid,
    long targetAmount,
    long totalDonated,
    long totalReleased,
    boolean isPaused,
    OffsetDateTime deployedAt
) {}
