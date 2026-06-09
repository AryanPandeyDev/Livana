package com.livana.backend.pool.service;

import com.livana.backend.ngo.entity.ApplicationStatus;
import com.livana.backend.ngo.repository.NgoApplicationRepository;
import com.livana.backend.pool.dto.PlatformStatsDto;
import com.livana.backend.pool.repository.PlatformStatsProjection;
import com.livana.backend.pool.repository.PoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PoolRepository poolRepository;
    private final NgoApplicationRepository ngoApplicationRepository;

    /**
     * Platform-wide stats.
     * PRD: "total donated, total released, number of pools, number of NGOs"
     *
     * - totalPoolsCount: ALL pools (not just unpaused)
     * - activePoolsCount: pools where isPaused = false
     * - verifiedNgosCount: NGO applications with APPROVED status
     */
    public PlatformStatsDto getPlatformStats() {
        PlatformStatsProjection stats = poolRepository.findPlatformStats();
        long verifiedNgos = ngoApplicationRepository.countByStatus(ApplicationStatus.APPROVED);

        return new PlatformStatsDto(
                stats.getTotalDonated() != null ? stats.getTotalDonated() : 0L,
                stats.getTotalReleased() != null ? stats.getTotalReleased() : 0L,
                stats.getTotalPoolsCount() != null ? stats.getTotalPoolsCount() : 0L,
                stats.getActivePoolsCount() != null ? stats.getActivePoolsCount() : 0L,
                verifiedNgos
        );
    }
}
