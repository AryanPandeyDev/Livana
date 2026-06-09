package com.livana.backend.pool.repository;

import com.livana.backend.pool.entity.Pool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for platform-stats aggregation and the pool listing filters.
 *
 * Stats mixes SUM, total COUNT, conditional active COUNT, and COALESCE-on-empty
 * — all easy to get subtly wrong (e.g. counting paused pools as active, or
 * NULL instead of 0 on an empty table). The region/title filters back the
 * public browse endpoint. H2 in PostgreSQL mode.
 *
 * Production behaviors protected:
 * - totalPoolsCount counts ALL pools; activePoolsCount counts only unpaused.
 * - totalDonated / totalReleased sum the running totals.
 * - empty table returns zeros, never NULL.
 * - region filter is case-insensitive; title search is a case-insensitive substring;
 *   combined filter is conjunctive.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PoolRepositoryTest {

    @Autowired
    private PoolRepository poolRepository;

    private static final OffsetDateTime TS =
            OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    private int seq = 0;

    private Pool pool(String title, String region, boolean paused,
                      long donated, long released, String creator) {
        seq++;
        String addr = String.format("0x%040d", seq);
        return Pool.builder()
                .onChainAddress(addr)
                .creatorAddress(creator)
                .poolIndex(seq)
                .metadataCid("cid" + seq)
                .title(title)
                .description("desc")
                .region(region)
                .targetAmount(1_000_000L)
                .totalDonated(donated)
                .totalReleased(released)
                .isPaused(paused)
                .deployTxHash("0xdeploy" + seq)
                .deployBlock(1000L + seq)
                .deployedAt(TS)
                .build();
    }

    // ========================================================================
    // Platform statistics
    // ========================================================================

    @Test
    @DisplayName("platform stats: total vs active pool counts and summed totals")
    void platformStatsCountsAndSums() {
        String ngo1 = "0xngo1111111111111111111111111111111111111";
        String ngo2 = "0xngo2222222222222222222222222222222222222";

        // 3 pools, 2 distinct creators, 1 paused.
        poolRepository.save(pool("A", "Asia", false, 100, 40, ngo1));
        poolRepository.save(pool("B", "Asia", true, 200, 80, ngo1)); // paused
        poolRepository.save(pool("C", "Africa", false, 300, 120, ngo2));
        poolRepository.flush();

        PlatformStatsProjection stats = poolRepository.findPlatformStats();

        assertThat(stats.getTotalPoolsCount()).isEqualTo(3L);
        assertThat(stats.getActivePoolsCount()).isEqualTo(2L); // excludes the paused one
        assertThat(stats.getTotalDonated()).isEqualTo(600L);
        assertThat(stats.getTotalReleased()).isEqualTo(240L);
    }

    @Test
    @DisplayName("platform stats on an empty table returns zeros, not null")
    void platformStatsEmptyReturnsZeros() {
        PlatformStatsProjection stats = poolRepository.findPlatformStats();

        assertThat(stats.getTotalDonated()).isZero();
        assertThat(stats.getTotalReleased()).isZero();
        assertThat(stats.getTotalPoolsCount()).isZero();
        assertThat(stats.getActivePoolsCount()).isZero();
    }

    // ========================================================================
    // Listing filters
    // ========================================================================

    @Test
    @DisplayName("region filter is case-insensitive")
    void regionFilterCaseInsensitive() {
        String ngo = "0xngo1111111111111111111111111111111111111";
        poolRepository.save(pool("Clean Water", "Asia", false, 0, 0, ngo));
        poolRepository.save(pool("School Build", "Africa", false, 0, 0, ngo));
        poolRepository.flush();

        Page<Pool> asia = poolRepository.findByRegionIgnoreCase("asia", PageRequest.of(0, 10));

        assertThat(asia.getTotalElements()).isEqualTo(1);
        assertThat(asia.getContent().get(0).getTitle()).isEqualTo("Clean Water");
    }

    @Test
    @DisplayName("title search matches a case-insensitive substring")
    void titleSearchSubstringCaseInsensitive() {
        String ngo = "0xngo1111111111111111111111111111111111111";
        poolRepository.save(pool("Clean Water Initiative", "Asia", false, 0, 0, ngo));
        poolRepository.save(pool("Reforestation", "Asia", false, 0, 0, ngo));
        poolRepository.flush();

        Page<Pool> result = poolRepository.findByTitleContainingIgnoreCase("water", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Water Initiative");
    }

    @Test
    @DisplayName("combined region + title filter is conjunctive (both must match)")
    void combinedFilterIsConjunctive() {
        String ngo = "0xngo1111111111111111111111111111111111111";
        poolRepository.save(pool("Water Asia", "Asia", false, 0, 0, ngo));    // matches both
        poolRepository.save(pool("Water Africa", "Africa", false, 0, 0, ngo)); // wrong region
        poolRepository.save(pool("School Asia", "Asia", false, 0, 0, ngo));    // wrong title
        poolRepository.flush();

        Page<Pool> result = poolRepository.findByRegionIgnoreCaseAndTitleContaining(
                "asia", "water", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Water Asia");
    }
}
