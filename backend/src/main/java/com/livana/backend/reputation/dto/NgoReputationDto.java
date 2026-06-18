package com.livana.backend.reputation.dto;

public record NgoReputationDto(
    String ngoAddress,
    /** Public display name of the verified NGO (from their approved application); null if not a verified NGO. */
    String orgName,
    long totalSbts,
    long totalAmountReleased,
    long poolCount
) {}
