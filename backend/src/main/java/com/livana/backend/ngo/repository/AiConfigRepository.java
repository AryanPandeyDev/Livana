package com.livana.backend.ngo.repository;

import com.livana.backend.ngo.entity.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiConfigRepository extends JpaRepository<AiConfig, UUID> {

    /** The single active AI config record holding the current encrypted Gemini key. */
    Optional<AiConfig> findFirstByActiveTrue();
}
