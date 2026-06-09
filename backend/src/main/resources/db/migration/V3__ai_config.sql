-- ============================================================================
-- V3__ai_config.sql
--
-- AI configuration: stores the encrypted Gemini API key (admin-swappable).
-- The key is encrypted with AES-GCM by the backend before storage; this table
-- never holds plaintext. At most one active record represents the current key.
-- ============================================================================

-- ============================================================================
-- 1. ai_config — encrypted Gemini API key + provenance
-- ============================================================================
CREATE TABLE ai_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encrypted_key   TEXT NOT NULL,
    set_by          VARCHAR(255) NOT NULL,
    set_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active       BOOLEAN NOT NULL DEFAULT true
);

-- Enforce at most one active record (the current key).
CREATE UNIQUE INDEX uq_ai_config_active ON ai_config(is_active) WHERE is_active = true;
