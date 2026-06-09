package com.livana.backend.reputation.dto;

public record NgoReputationDto(
    String ngoAddress,
    long totalSbts,
    long totalAmountReleased,
    long poolCount
) {}
