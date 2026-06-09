package com.livana.backend.donation.dto;

import java.time.OffsetDateTime;

public record DonorDonationDto(
    String poolAddress,
    long amount,
    String txHash,
    OffsetDateTime blockTimestamp
) {}
