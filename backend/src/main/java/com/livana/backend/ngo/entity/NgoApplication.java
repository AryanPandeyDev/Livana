package com.livana.backend.ngo.entity;

import com.livana.backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ngo_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NgoApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "official_email", nullable = false)
    private String officialEmail;

    @Column(name = "documents_cid")
    private String documentsCid;

    @Column(name = "wallet_address", nullable = false, length = 42)
    private String walletAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "ai_confidence_score", precision = 5, scale = 2)
    private BigDecimal aiConfidenceScore;

    @Column(name = "ai_research_summary", columnDefinition = "TEXT")
    private String aiResearchSummary;

    @Column(name = "ai_verdict", length = 10)
    private String aiVerdict;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
