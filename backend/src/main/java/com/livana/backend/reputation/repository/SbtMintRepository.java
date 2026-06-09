package com.livana.backend.reputation.repository;

import com.livana.backend.reputation.entity.SbtMint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SbtMintRepository extends JpaRepository<SbtMint, UUID> {

    List<SbtMint> findByNgoAddress(String ngoAddress);

    boolean existsByTokenId(Long tokenId);

    Page<SbtMint> findByNgoAddress(String ngoAddress, Pageable pageable);

    @Query("""
        SELECT s.ngoAddress AS ngoAddress,
               COUNT(s) AS totalSbts,
               SUM(s.amount) AS totalAmountReleased,
               COUNT(DISTINCT s.poolAddress) AS poolCount
        FROM SbtMint s
        GROUP BY s.ngoAddress
        ORDER BY SUM(s.amount) DESC, s.ngoAddress ASC
        """)
    List<NgoLeaderboardProjection> findNgoLeaderboard(Pageable pageable);

    @Query("""
        SELECT COUNT(s) AS totalSbts,
               COALESCE(SUM(s.amount), 0) AS totalAmountReleased,
               COUNT(DISTINCT s.poolAddress) AS poolCount
        FROM SbtMint s
        WHERE s.ngoAddress = :ngoAddress
        """)
    NgoReputationProjection findReputationByNgoAddress(@Param("ngoAddress") String ngoAddress);
}
