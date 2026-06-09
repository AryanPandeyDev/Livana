package com.livana.backend.donation.dto;

public record LeaderboardEntryDto(
    String donorAddress,
    long totalDonated,
    long donationCount
) {}
