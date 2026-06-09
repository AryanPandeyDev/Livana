-- ============================================================================
-- V2__remove_otp_and_rename_roles.sql
--
-- 1. Remove OTP email verification columns from ngo_applications.
--    Email verification is now handled by Clerk. The backend no longer
--    generates OTPs or manages the EMAIL_VERIFICATION status.
--
-- 2. Rename DONOR role to USER across users table.
--    USER is the default authenticated role — any user can donate and
--    apply as NGO. DONOR was misleading.
--
-- New status machine: DRAFT → AI_SCREENING → PENDING_REVIEW → APPROVED / REJECTED
-- New roles: USER, NGO, ADMIN
-- ============================================================================

-- ============================================================================
-- Step 1: Drop OTP columns
-- ============================================================================
ALTER TABLE ngo_applications DROP COLUMN IF EXISTS otp_hash;
ALTER TABLE ngo_applications DROP COLUMN IF EXISTS otp_expires_at;

-- ============================================================================
-- Step 2: Migrate any applications stuck in EMAIL_VERIFICATION back to DRAFT
-- (must happen before the constraint is updated)
-- ============================================================================
UPDATE ngo_applications SET status = 'DRAFT' WHERE status = 'EMAIL_VERIFICATION';

-- ============================================================================
-- Step 3: Update status constraint to remove EMAIL_VERIFICATION
-- ============================================================================
ALTER TABLE ngo_applications DROP CONSTRAINT IF EXISTS chk_application_status;
ALTER TABLE ngo_applications ADD CONSTRAINT chk_application_status CHECK (
    status IN ('DRAFT', 'AI_SCREENING', 'PENDING_REVIEW', 'APPROVED', 'REJECTED')
);

-- ============================================================================
-- Step 4: Rename DONOR role to USER
-- ============================================================================
UPDATE users SET role = 'USER' WHERE role = 'DONOR';

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (
    role IN ('USER', 'NGO', 'ADMIN')
);

-- Update the default
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'USER';
