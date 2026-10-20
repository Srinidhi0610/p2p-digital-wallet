-- ===================================================================
-- Phase 1 Migration: Transfer Idempotency & Ledger Immutability Alignment
-- Safe, non-destructive migration for existing tables
-- ===================================================================

BEGIN;

-- 1. Add idempotency_key column and scoped unique constraint to transfers
ALTER TABLE transfers 
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100) NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_transfers_sender_idempotency'
    ) THEN
        ALTER TABLE transfers 
            ADD CONSTRAINT uk_transfers_sender_idempotency UNIQUE (sender_wallet_id, idempotency_key);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_transfers_sender_idempotency 
    ON transfers(sender_wallet_id, idempotency_key);

-- 2. Add currency column to ledger_entries
ALTER TABLE ledger_entries 
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL;

-- 3. Replace cascade delete on fk_ledger_transfer with ON DELETE RESTRICT
ALTER TABLE ledger_entries 
    DROP CONSTRAINT IF EXISTS fk_ledger_transfer;

ALTER TABLE ledger_entries 
    ADD CONSTRAINT fk_ledger_transfer 
    FOREIGN KEY (transfer_id) REFERENCES transfers(id) ON DELETE RESTRICT;

COMMIT;
