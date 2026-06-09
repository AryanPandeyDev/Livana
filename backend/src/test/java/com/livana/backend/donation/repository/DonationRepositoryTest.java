package com.livana.backend.donation.repository;

import com.livana.backend.donation.entity.Donation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aggregation-correctness tests for the donor leaderboard query.
 *
 * The leaderboard is hand-written JPQL with SUM / COUNT / GROUP BY / ORDER BY
 * plus a deterministic tiebreaker — the most bug-prone category in the codebase
 * (wrong totals, wrong ordering, broken tiebreaker). H2 in PostgreSQL mode.
 *
 * Production behaviors protected:
 * - one row per distinct donor, totalDonated = SUM(amount), donationCount = COUNT.
 * - ordering is by totalDonated DESC.
 * - ties broken by donorAddress ASC (deterministic).
 * - the limit (PageRequest size) caps the number of entries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DonationRepositoryTest {

    @Autowired
    private DonationRepository donationRepository;

    private static final OffsetDateTime TS =
            OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    private static final String POOL_A = "0xaaaa000000000000000000000000000000000001";
    private static final String POOL_B = "0xaaaa000000000000000000000000000000000002";

    private void donation(String donor, String pool, long amount, String txHash, int logIndex) {
        donationRepository.save(Donation.builder()
                .poolAddress(pool)
                .donorAddress(donor)
                .amount(amount)
                .txHash(txHash)
                .logIndex(logIndex)
                .blockNumber(1000L)
                .blockTimestamp(TS)
                .build());
    }

    @Test
    @DisplayName("aggregates per donor: sum of amounts and donation count")
    void aggregatesPerDonor() {
        String donor1 = "0x1111111111111111111111111111111111111111";
        String donor2 = "0x2222222222222222222222222222222222222222";

        // donor1: two donations across two pools → total 300, count 2
        donation(donor1, POOL_A, 100, "0xtx1", 0);
        donation(donor1, POOL_B, 200, "0xtx2", 0);
        // donor2: one donation → total 50, count 1
        donation(donor2, POOL_A, 50, "0xtx3", 0);
        donationRepository.flush();

        List<LeaderboardProjection> board =
                donationRepository.findDonorLeaderboard(PageRequest.of(0, 10));

        assertThat(board).hasSize(2);

        // Ordered by total DESC → donor1 first
        LeaderboardProjection top = board.get(0);
        assertThat(top.getDonorAddress()).isEqualTo(donor1);
        assertThat(top.getTotalDonated()).isEqualTo(300L);
        assertThat(top.getDonationCount()).isEqualTo(2L);

        LeaderboardProjection second = board.get(1);
        assertThat(second.getDonorAddress()).isEqualTo(donor2);
        assertThat(second.getTotalDonated()).isEqualTo(50L);
        assertThat(second.getDonationCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("orders by total donated descending")
    void ordersByTotalDescending() {
        donation("0xaaa1111111111111111111111111111111111111", POOL_A, 10, "0xa", 0);
        donation("0xbbb2222222222222222222222222222222222222", POOL_A, 999, "0xb", 0);
        donation("0xccc3333333333333333333333333333333333333", POOL_A, 500, "0xc", 0);
        donationRepository.flush();

        List<LeaderboardProjection> board =
                donationRepository.findDonorLeaderboard(PageRequest.of(0, 10));

        assertThat(board).extracting(LeaderboardProjection::getTotalDonated)
                .containsExactly(999L, 500L, 10L);
    }

    @Test
    @DisplayName("breaks ties by donor address ascending for deterministic ordering")
    void breaksTiesByAddressAscending() {
        // Three donors with identical totals; addresses chosen so ASC order is clear.
        String low = "0x1000000000000000000000000000000000000000";
        String mid = "0x5000000000000000000000000000000000000000";
        String high = "0x9000000000000000000000000000000000000000";

        // Insert in non-sorted order to prove the query sorts, not insertion order.
        donation(high, POOL_A, 100, "0xh", 0);
        donation(low, POOL_A, 100, "0xl", 0);
        donation(mid, POOL_A, 100, "0xm", 0);
        donationRepository.flush();

        List<LeaderboardProjection> board =
                donationRepository.findDonorLeaderboard(PageRequest.of(0, 10));

        assertThat(board).extracting(LeaderboardProjection::getDonorAddress)
                .containsExactly(low, mid, high);
    }

    @Test
    @DisplayName("limit caps the number of leaderboard entries")
    void limitCapsEntries() {
        for (int i = 1; i <= 5; i++) {
            String donor = "0x" + Integer.toString(i).repeat(40).substring(0, 40);
            donation(donor, POOL_A, i * 100L, "0xtx" + i, 0);
        }
        donationRepository.flush();

        List<LeaderboardProjection> top3 =
                donationRepository.findDonorLeaderboard(PageRequest.of(0, 3));

        assertThat(top3).hasSize(3);
        // Highest three totals: 500, 400, 300
        assertThat(top3).extracting(LeaderboardProjection::getTotalDonated)
                .containsExactly(500L, 400L, 300L);
    }

    @Test
    @DisplayName("empty table yields an empty leaderboard")
    void emptyTableEmptyLeaderboard() {
        assertThat(donationRepository.findDonorLeaderboard(PageRequest.of(0, 10)))
                .isEmpty();
    }
}
