package com.livana.backend.ngo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "encrypted_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedKey;

    @Column(name = "set_by", nullable = false)
    private String setBy;

    @Column(name = "set_at", nullable = false)
    private OffsetDateTime setAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
