package com.livana.backend.donation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Denormalized from DonationReceived events. One row per donation.
 * Maps to the donations table.
 */
@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pool_address", nullable = false, length = 42)
    private String poolAddress;

    @Column(name = "donor_address", nullable = false, length = 42)
    private String donorAddress;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    @Column(name = "log_index", nullable = false)
    private Integer logIndex;

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
