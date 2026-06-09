package com.livana.backend.donation.repository;

import com.livana.backend.donation.entity.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {

    Page<Donation> findByPoolAddress(String poolAddress, Pageable pageable);

    Page<Donation> findByDonorAddress(String donorAddress, Pageable pageable);

    boolean existsByTxHashAndLogIndex(String txHash, Integer logIndex);

    @Query("""
        SELECT d.donorAddress AS donorAddress,
               SUM(d.amount) AS totalDonated,
               COUNT(d) AS donationCount
        FROM Donation d
        GROUP BY d.donorAddress
        ORDER BY SUM(d.amount) DESC, d.donorAddress ASC
        """)
    List<LeaderboardProjection> findDonorLeaderboard(Pageable pageable);
}
