package com.livana.backend.indexer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks the last processed block per contract for backfill on restart.
 * Maps to the indexer_state table.
 */
@Entity
@Table(name = "indexer_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexerState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contract_address", nullable = false, unique = true, length = 42)
    private String contractAddress;

    @Column(name = "contract_type", nullable = false, length = 20)
    private String contractType;

    @Column(name = "last_indexed_block", nullable = false)
    private Long lastIndexedBlock;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
