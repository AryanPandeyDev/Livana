-- ============================================================================
-- V1__initial_schema.sql
-- Livana Backend — Complete initial database schema
-- ============================================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. users — Clerk-synced user profiles
-- ============================================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clerk_id        VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    display_name    VARCHAR(255),
    wallet_address  VARCHAR(42) UNIQUE,
    role            VARCHAR(20) NOT NULL DEFAULT 'DONOR',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_users_role CHECK (role IN ('DONOR', 'NGO', 'ADMIN'))
);

-- ============================================================================
-- 2. ngo_applications — NGO onboarding applications
-- ============================================================================
CREATE TABLE ngo_applications (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id),
    org_name                VARCHAR(255) NOT NULL,
    registration_number     VARCHAR(100) NOT NULL,
    description             TEXT NOT NULL,
    official_email          VARCHAR(255) NOT NULL,
    documents_cid           VARCHAR(255),
    wallet_address          VARCHAR(42) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    otp_hash                VARCHAR(255),
    otp_expires_at          TIMESTAMPTZ,
    ai_confidence_score     DECIMAL(5,2),
    ai_research_summary     TEXT,
    ai_verdict              VARCHAR(10),
    admin_notes             TEXT,
    rejection_reason        TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_application_status CHECK (
        status IN ('DRAFT', 'EMAIL_VERIFICATION', 'AI_SCREENING', 'PENDING_REVIEW', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT chk_ai_verdict CHECK (ai_verdict IS NULL OR ai_verdict IN ('PASS', 'FAIL'))
);

CREATE INDEX idx_ngo_applications_user_id ON ngo_applications(user_id);
CREATE INDEX idx_ngo_applications_wallet ON ngo_applications(wallet_address);
CREATE INDEX idx_ngo_applications_status ON ngo_applications(status);

-- Only one active (non-terminal) application per user at a time
CREATE UNIQUE INDEX uq_ngo_applications_active_user
    ON ngo_applications(user_id)
    WHERE status NOT IN ('APPROVED', 'REJECTED');

-- ============================================================================
-- 3. pools — Cached IPFS metadata for indexed on-chain pools
-- ============================================================================
CREATE TABLE pools (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    on_chain_address    VARCHAR(42) NOT NULL UNIQUE,
    creator_address     VARCHAR(42) NOT NULL,
    pool_index          INTEGER NOT NULL,
    metadata_cid        VARCHAR(255) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT NOT NULL,
    region              VARCHAR(100) NOT NULL,
    cover_image_cid     VARCHAR(255),
    target_amount       BIGINT NOT NULL,
    total_donated       BIGINT NOT NULL DEFAULT 0,
    total_released      BIGINT NOT NULL DEFAULT 0,
    is_paused           BOOLEAN NOT NULL DEFAULT false,
    deploy_tx_hash      VARCHAR(66) NOT NULL,
    deploy_block        BIGINT NOT NULL,
    deployed_at         TIMESTAMPTZ NOT NULL,
    indexed_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pools_creator ON pools(creator_address);
CREATE INDEX idx_pools_region ON pools(region);

-- ============================================================================
-- 4. donations — Denormalized from DonationReceived events
-- ============================================================================
CREATE TABLE donations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pool_address    VARCHAR(42) NOT NULL,
    donor_address   VARCHAR(42) NOT NULL,
    amount          BIGINT NOT NULL,
    tx_hash         VARCHAR(66) NOT NULL,
    log_index       INTEGER NOT NULL,
    block_number    BIGINT NOT NULL,
    block_timestamp TIMESTAMPTZ NOT NULL,
    indexed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_donations_event UNIQUE (tx_hash, log_index)
);

CREATE INDEX idx_donations_pool ON donations(pool_address);
CREATE INDEX idx_donations_donor ON donations(donor_address);
CREATE INDEX idx_donations_leaderboard ON donations(donor_address, amount);

-- ============================================================================
-- 5. proofs — Denormalized from ProofSubmitted + FundsReleased events
-- ============================================================================
CREATE TABLE proofs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pool_address        VARCHAR(42) NOT NULL,
    proof_id            INTEGER NOT NULL,
    ipfs_cid            VARCHAR(255) NOT NULL,
    amount              BIGINT NOT NULL,
    released            BOOLEAN NOT NULL DEFAULT false,
    submitted_tx_hash   VARCHAR(66) NOT NULL,
    submitted_block     BIGINT NOT NULL,
    submitted_at        TIMESTAMPTZ NOT NULL,
    released_tx_hash    VARCHAR(66),
    released_block      BIGINT,
    released_at         TIMESTAMPTZ,
    indexed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_proofs_pool_proof UNIQUE (pool_address, proof_id)
);

CREATE INDEX idx_proofs_pool ON proofs(pool_address);
CREATE INDEX idx_proofs_pending ON proofs(released) WHERE released = false;

-- ============================================================================
-- 6. sbt_mints — Denormalized from Locked events + on-chain reputation data
-- ============================================================================
CREATE TABLE sbt_mints (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id        BIGINT NOT NULL UNIQUE,
    ngo_address     VARCHAR(42) NOT NULL,
    pool_address    VARCHAR(42) NOT NULL,
    amount          BIGINT NOT NULL,
    tx_hash         VARCHAR(66) NOT NULL,
    block_number    BIGINT NOT NULL,
    block_timestamp TIMESTAMPTZ NOT NULL,
    indexed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sbt_mints_ngo ON sbt_mints(ngo_address);
CREATE INDEX idx_sbt_mints_pool ON sbt_mints(pool_address);

-- ============================================================================
-- 7. indexed_events — Raw event log for auditability
-- ============================================================================
CREATE TABLE indexed_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type          VARCHAR(30) NOT NULL,
    contract_address    VARCHAR(42) NOT NULL,
    tx_hash             VARCHAR(66) NOT NULL,
    log_index           INTEGER NOT NULL,
    block_number        BIGINT NOT NULL,
    block_timestamp     TIMESTAMPTZ NOT NULL,
    raw_data            JSONB NOT NULL,
    indexed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_indexed_events UNIQUE (tx_hash, log_index),
    CONSTRAINT chk_event_type CHECK (
        event_type IN (
            'NGO_APPROVED', 'NGO_REVOKED', 'POOL_DEPLOYED',
            'DONATION_RECEIVED', 'PROOF_SUBMITTED', 'FUNDS_RELEASED',
            'SBT_LOCKED', 'MULTI_SIG_ADMIN_SET', 'PAUSED', 'UNPAUSED'
        )
    )
);

CREATE INDEX idx_events_type ON indexed_events(event_type);
CREATE INDEX idx_events_contract ON indexed_events(contract_address);
CREATE INDEX idx_events_block ON indexed_events(block_number);

-- ============================================================================
-- 8. indexer_state — Tracks last processed block per contract
-- ============================================================================
CREATE TABLE indexer_state (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_address    VARCHAR(42) NOT NULL UNIQUE,
    contract_type       VARCHAR(20) NOT NULL,
    last_indexed_block  BIGINT NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_contract_type CHECK (
        contract_type IN ('POOL_FACTORY', 'FUND_POOL', 'SBT')
    )
);

-- ============================================================================
-- Auto-update updated_at timestamps
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_ngo_applications_updated_at
    BEFORE UPDATE ON ngo_applications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
