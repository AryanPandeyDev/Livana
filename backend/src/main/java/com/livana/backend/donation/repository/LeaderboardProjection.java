package com.livana.backend.donation.repository;

public interface LeaderboardProjection {
    String getDonorAddress();
    Long getTotalDonated();
    Long getDonationCount();
}
