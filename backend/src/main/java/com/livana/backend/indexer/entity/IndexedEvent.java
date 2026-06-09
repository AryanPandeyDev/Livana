package com.livana.backend.indexer.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Raw event log — every on-chain event is stored here for auditability.
 * Maps to the indexed_events table.
 */
@Entity
@Table(name = "indexed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;

    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    @Column(name = "log_index", nullable = false)
    private Integer logIndex;

    @Column(name = "block_number", nullable = false)
    private Long blockNumber;

    @Column(name = "block_timestamp", nullable = false)
    private OffsetDateTime blockTimestamp;

    /**
     * Decoded event parameters stored as JSONB.
     * Using JsonNode so Hibernate serializes directly to PG jsonb
     * without an intermediate String round-trip.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false, columnDefinition = "JSONB")
    private JsonNode rawData;

    @Column(name = "indexed_at", nullable = false, updatable = false)
    private OffsetDateTime indexedAt;

    @PrePersist
    protected void onCreate() {
        indexedAt = OffsetDateTime.now();
    }
}
