package com.livana.backend.pool.repository;

public interface PlatformStatsProjection {
    Long getTotalDonated();
    Long getTotalReleased();
    Long getTotalPoolsCount();
    Long getActivePoolsCount();
}
