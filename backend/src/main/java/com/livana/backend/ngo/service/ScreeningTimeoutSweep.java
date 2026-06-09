package com.livana.backend.ngo.service;

import com.livana.backend.ngo.config.AiScreeningProperties;
import com.livana.backend.ngo.entity.ApplicationStatus;
import com.livana.backend.ngo.repository.NgoApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled fallback: advances applications stuck in AI_SCREENING past the
 * callback-timeout window to PENDING_REVIEW with null AI results, so a missing
 * or never-delivered callback never strands a legitimate NGO.
 *
 * Uses the same atomic conditional update as the callback, so the sweep and a
 * late callback cannot double-apply — whichever runs first wins.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScreeningTimeoutSweep {

    private final NgoApplicationRepository applicationRepository;
    private final NgoApplicationService ngoApplicationService;
    private final AiScreeningProperties properties;

    /** Runs every 30 seconds. */
    @Scheduled(fixedDelayString = "30000")
    public void sweepStaleScreenings() {
        OffsetDateTime threshold = OffsetDateTime.now()
                .minusSeconds(properties.getCallbackTimeoutSeconds());
        List<UUID> staleIds = applicationRepository.findStaleScreeningIds(
                ApplicationStatus.AI_SCREENING, threshold);
        if (staleIds.isEmpty()) {
            return;
        }
        log.info("Timeout sweep: advancing {} stale AI_SCREENING application(s) to PENDING_REVIEW",
                staleIds.size());
        for (UUID id : staleIds) {
            try {
                ngoApplicationService.fallbackToManualReview(id);
            } catch (Exception e) {
                log.warn("Failed to sweep application {}: {}", id, e.getMessage());
            }
        }
    }
}
