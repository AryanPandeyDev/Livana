package com.livana.backend.pool.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cached IPFS metadata for indexed on-chain pools.
 * Maps to the pools table.
 *
 * From PRD: "Backend serves pool data from its cache — not from IPFS on every request."
 * The indexer fetches metadata from IPFS when a PoolDeployed event is processed,
 * validates the JSON schema, and inserts a row here. Invalid metadata = no row.
 */
@Entity
@Table(name = "pools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pool {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "on_chain_address", nullable = false, unique = true, length = 42)
    private String onChainAddress;

    @Column(name = "creator_address", nullable = false, length = 42)
    private String creatorAddress;

    @Column(name = "pool_index", nullable = false)
    private Integer poolIndex;

    @Column(name = "metadata_cid", nullable = false)
    private String metadataCid;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(name = "cover_image_cid")
    private String coverImageCid;

    @Column(name = "target_amount", nullable = false)
    private Long targetAmount;

    @Column(name = "total_donated", nullable = false)
    @Builder.Default
    private Long totalDonated = 0L;

    @Column(name = "total_released", nullable = false)
    @Builder.Default
    private Long totalReleased = 0L;

    @Column(name = "is_paused", nullable = false)
    @Builder.Default
    private Boolean isPaused = false;

    @Column(name = "deploy_tx_hash", nullable = false, length = 66)
    private String deployTxHash;

    @Column(name = "deploy_block", nullable = false)
    private Long deployBlock;

    @Column(name = "deployed_at", nullable = false)
    private OffsetDateTime deployedAt;

    @Column(name = "indexed_at", nullable = false, updatable = false)
    private OffsetDateTime indexedAt;

    @PrePersist
    protected void onCreate() {
        indexedAt = OffsetDateTime.now();
    }
}
