package com.livana.backend.donation.dto;

import java.time.OffsetDateTime;

public record PoolDonationDto(
    String donorAddress,
    long amount,
    String txHash,
    OffsetDateTime blockTimestamp
) {}
