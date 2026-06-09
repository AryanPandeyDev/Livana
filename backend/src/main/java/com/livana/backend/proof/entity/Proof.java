package com.livana.backend.proof.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Denormalized from ProofSubmitted + FundsReleased events.
 * Maps to the proofs table.
 *
 * From PRD: "proofs.released is updated in-place when a FundsReleased event
 * fires for a matching (pool_address, proof_id). No separate release table."
 */
@Entity
@Table(name = "proofs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proof {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pool_address", nullable = false, length = 42)
    private String poolAddress;

    @Column(name = "proof_id", nullable = false)
    private Integer proofId;

    @Column(name = "ipfs_cid", nullable = false)
    private String ipfsCid;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    @Builder.Default
    private Boolean released = false;

    @Column(name = "submitted_tx_hash", nullable = false, length = 66)
    private String submittedTxHash;

    @Column(name = "submitted_block", nullable = false)
    private Long submittedBlock;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "released_tx_hash", length = 66)
    private String releasedTxHash;

    @Column(name = "released_block")
    private Long releasedBlock;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "indexed_at", nullable = false, updatable = false)
    private OffsetDateTime indexedAt;

    @PrePersist
    protected void onCreate() {
        indexedAt = OffsetDateTime.now();
    }
}
