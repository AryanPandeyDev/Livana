package com.livana.backend.pool.repository;

import com.livana.backend.pool.entity.Pool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PoolRepository extends JpaRepository<Pool, UUID> {

    Optional<Pool> findByOnChainAddress(String onChainAddress);

    boolean existsByOnChainAddress(String onChainAddress);

    Page<Pool> findByRegion(String region, Pageable pageable);

    Page<Pool> findAllBy(Pageable pageable);

    /**
     * Get all known pool on-chain addresses — used by the indexer on startup
     * to register existing pools for event tracking.
     */
    List<Pool> findAll();

    /**
     * Aggregates all pools for platform-wide statistics.
     * Note: verifiedNgosCount is sourced from NgoApplicationRepository, not here.
     */
    @Query("""
        SELECT COALESCE(SUM(p.totalDonated), 0) AS totalDonated,
               COALESCE(SUM(p.totalReleased), 0) AS totalReleased,
               COUNT(p) AS totalPoolsCount,
               COUNT(CASE WHEN p.isPaused = false THEN 1 END) AS activePoolsCount
        FROM Pool p
        """)
    PlatformStatsProjection findPlatformStats();

    /**
     * Filter pools by region (case-insensitive).
     */
    Page<Pool> findByRegionIgnoreCase(String region, Pageable pageable);

    /**
     * Search pools by title substring (case-insensitive).
     */
    @Query("SELECT p FROM Pool p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Pool> findByTitleContainingIgnoreCase(@Param("search") String search, Pageable pageable);

    /**
     * Combined region + title search (both case-insensitive).
     */
    @Query("""
        SELECT p FROM Pool p
        WHERE LOWER(p.region) = LOWER(:region)
          AND LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<Pool> findByRegionIgnoreCaseAndTitleContaining(@Param("region") String region, @Param("search") String search, Pageable pageable);
}
