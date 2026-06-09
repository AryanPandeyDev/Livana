package com.livana.backend.reputation.repository;

public interface NgoLeaderboardProjection {
    String getNgoAddress();
    Long getTotalSbts();
    Long getTotalAmountReleased();
    Long getPoolCount();
}
