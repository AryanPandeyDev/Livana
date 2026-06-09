package com.livana.backend.reputation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Denormalized from Locked events + on-chain reputation data.
 * Maps to the sbt_mints table. One row per SBT.
 *
 * From PRD: "SBT data comes from the Locked event + an on-chain getReputation() call.
 * The Locked event only has tokenId — the indexer calls sbt.getReputation(tokenId)
 * and sbt.ownerOf(tokenId) to get the full data."
 */
@Entity
@Table(name = "sbt_mints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbtMint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_id", nullable = false, unique = true)
    private Long tokenId;

    @Column(name = "ngo_address", nullable = false, length = 42)
    private String ngoAddress;

    @Column(name = "pool_address", nullable = false, length = 42)
    private String poolAddress;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    @Column(name = "block_number", nullable = false)
    private Long blockNumber;

    @Column(name = "block_timestamp", nullable = false)
    private OffsetDateTime blockTimestamp;

    @Column(name = "indexed_at", nullable = false, updatable = false)
    private OffsetDateTime indexedAt;

    @PrePersist
    protected void onCreate() {
        indexedAt = OffsetDateTime.now();
    }
}
