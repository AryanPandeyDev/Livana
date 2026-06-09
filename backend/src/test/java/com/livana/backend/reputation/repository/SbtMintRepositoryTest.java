package com.livana.backend.reputation.repository;

import com.livana.backend.reputation.entity.SbtMint;
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
 * Aggregation-correctness tests for NGO reputation queries.
 *
 * Reputation is the trickiest aggregation: per NGO it sums amounts, counts SBTs,
 * AND counts DISTINCT pools — a COUNT(DISTINCT ...) bug is easy to miss and would
 * misreport how many successful pools an NGO has. H2 in PostgreSQL mode.
 *
 * Production behaviors protected:
 * - single-NGO reputation: totalSbts = COUNT, totalAmountReleased = SUM,
 *   poolCount = COUNT(DISTINCT poolAddress).
 * - distinct pool counting (two SBTs from the same pool count as one pool).
 * - leaderboard ordering by total released DESC with address tiebreaker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SbtMintRepositoryTest {

    @Autowired
    private SbtMintRepository sbtMintRepository;

    private static final OffsetDateTime TS =
            OffsetDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    private long nextToken = 1L;

    private void mint(String ngo, String pool, long amount) {
        sbtMintRepository.save(SbtMint.builder()
                .tokenId(nextToken++)
                .ngoAddress(ngo)
                .poolAddress(pool)
                .amount(amount)
                .txHash("0xtx" + nextToken)
                .blockNumber(1000L)
                .blockTimestamp(TS)
                .build());
    }

    @Test
    @DisplayName("single NGO reputation: count, sum, and distinct pool count")
    void singleNgoReputation() {
        String ngo = "0x1111111111111111111111111111111111111111";
        String poolA = "0xpoolaaa00000000000000000000000000000001";
        String poolB = "0xpoolbbb00000000000000000000000000000002";

        // 3 SBTs but only 2 distinct pools (two from poolA).
        mint(ngo, poolA, 100);
        mint(ngo, poolA, 150);
        mint(ngo, poolB, 250);
        sbtMintRepository.flush();

        NgoReputationProjection rep = sbtMintRepository.findReputationByNgoAddress(ngo);

        assertThat(rep.getTotalSbts()).isEqualTo(3L);
        assertThat(rep.getTotalAmountReleased()).isEqualTo(500L);
        assertThat(rep.getPoolCount()).isEqualTo(2L); // distinct pools, not 3
    }

    @Test
    @DisplayName("reputation excludes other NGOs' mints")
    void reputationIsScopedToNgo() {
        String ngo = "0x1111111111111111111111111111111111111111";
        String other = "0x2222222222222222222222222222222222222222";
        String pool = "0xpoolaaa00000000000000000000000000000001";

        mint(ngo, pool, 100);
        mint(other, pool, 9999);
        sbtMintRepository.flush();

        NgoReputationProjection rep = sbtMintRepository.findReputationByNgoAddress(ngo);

        assertThat(rep.getTotalSbts()).isEqualTo(1L);
        assertThat(rep.getTotalAmountReleased()).isEqualTo(100L);
    }

    @Test
    @DisplayName("leaderboard ranks NGOs by total released descending with distinct pool counts")
    void leaderboardRanksByTotalReleased() {
        String ngoTop = "0xaaa1111111111111111111111111111111111111";
        String ngoMid = "0xbbb2222222222222222222222222222222222222";
        String poolA = "0xpoolaaa00000000000000000000000000000001";
        String poolB = "0xpoolbbb00000000000000000000000000000002";

        // ngoTop: 600 across 2 distinct pools, 2 SBTs
        mint(ngoTop, poolA, 300);
        mint(ngoTop, poolB, 300);
        // ngoMid: 100 across 1 pool, 1 SBT
        mint(ngoMid, poolA, 100);
        sbtMintRepository.flush();

        List<NgoLeaderboardProjection> board =
                sbtMintRepository.findNgoLeaderboard(PageRequest.of(0, 10));

        assertThat(board).hasSize(2);

        NgoLeaderboardProjection top = board.get(0);
        assertThat(top.getNgoAddress()).isEqualTo(ngoTop);
        assertThat(top.getTotalAmountReleased()).isEqualTo(600L);
        assertThat(top.getTotalSbts()).isEqualTo(2L);
        assertThat(top.getPoolCount()).isEqualTo(2L);

        assertThat(board.get(1).getNgoAddress()).isEqualTo(ngoMid);
        assertThat(board.get(1).getTotalAmountReleased()).isEqualTo(100L);
    }

    @Test
    @DisplayName("leaderboard breaks ties by NGO address ascending")
    void leaderboardTieBreak() {
        String low = "0x1000000000000000000000000000000000000000";
        String high = "0x9000000000000000000000000000000000000000";
        String pool = "0xpoolaaa00000000000000000000000000000001";

        // Equal totals; insert high first to prove ordering is by address, not insertion.
        mint(high, pool, 500);
        mint(low, pool, 500);
        sbtMintRepository.flush();

        List<NgoLeaderboardProjection> board =
                sbtMintRepository.findNgoLeaderboard(PageRequest.of(0, 10));

        assertThat(board).extracting(NgoLeaderboardProjection::getNgoAddress)
                .containsExactly(low, high);
    }
}
