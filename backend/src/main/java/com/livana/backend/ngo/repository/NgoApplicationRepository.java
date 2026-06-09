package com.livana.backend.ngo.repository;

import com.livana.backend.ngo.entity.ApplicationStatus;
import com.livana.backend.ngo.entity.NgoApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NgoApplicationRepository extends JpaRepository<NgoApplication, UUID> {

    /**
     * Find the user's active (non-terminal) application.
     * The DB partial unique index guarantees at most one result.
     */
    Optional<NgoApplication> findByUserIdAndStatusNotIn(UUID userId, java.util.Collection<ApplicationStatus> terminalStatuses);

    /**
     * Find the user's most recent application regardless of status.
     * Used by GET /me so NGOs can see their APPROVED/REJECTED status.
     */
    Optional<NgoApplication> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find an application by wallet address that is in PENDING_REVIEW state.
     * Used by the Event Indexer when processing on-chain NGOApproved events.
     * Restricted to PENDING_REVIEW to ensure the on-chain approval doesn't
     * bypass the backend workflow (email verification, AI screening).
     */
    Optional<NgoApplication> findByWalletAddressAndStatus(String walletAddress, ApplicationStatus status);

    /**
     * Admin: list applications filtered by status, paginated.
     */
    Page<NgoApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    /**
     * Admin: list all applications, paginated.
     */
    Page<NgoApplication> findAllBy(Pageable pageable);

    /**
     * Count verified (approved) NGOs for platform stats.
     */
    long countByStatus(ApplicationStatus status);

    /** Callback success: store results and advance, only if still in fromStatus. Returns rows affected (1 = this call won). */
    @Modifying
    @Query("""
        UPDATE NgoApplication a
           SET a.status = :toStatus,
               a.aiConfidenceScore = :score,
               a.aiResearchSummary = :summary,
               a.aiVerdict = :verdict
         WHERE a.id = :id AND a.status = :fromStatus
    """)
    int completeScreening(@Param("id") UUID id,
                          @Param("fromStatus") ApplicationStatus fromStatus,
                          @Param("toStatus") ApplicationStatus toStatus,
                          @Param("score") BigDecimal score,
                          @Param("summary") String summary,
                          @Param("verdict") String verdict);

    /** Fallback: advance with null AI results, only if still in fromStatus. Returns rows affected. */
    @Modifying
    @Query("""
        UPDATE NgoApplication a SET a.status = :toStatus
         WHERE a.id = :id AND a.status = :fromStatus
    """)
    int fallbackToPendingReview(@Param("id") UUID id,
                                @Param("fromStatus") ApplicationStatus fromStatus,
                                @Param("toStatus") ApplicationStatus toStatus);

    /** Timeout sweep: ids still stuck in the given status past the threshold. */
    @Query("SELECT a.id FROM NgoApplication a WHERE a.status = :status AND a.updatedAt < :threshold")
    List<UUID> findStaleScreeningIds(@Param("status") ApplicationStatus status,
                                     @Param("threshold") OffsetDateTime threshold);
}
