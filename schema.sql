-- ===================================================================
-- Initial Database Schema for P2P Digital Wallet and Transfer API
-- PostgreSQL 14+ compatible DDL
-- ===================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 2. Wallets Table
CREATE TABLE IF NOT EXISTS wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uk_wallets_user_currency UNIQUE (user_id, currency),
    CONSTRAINT chk_wallet_balance_positive CHECK (balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_wallets_user_currency ON wallets(user_id, currency);

-- 3. Transfers Table
CREATE TABLE IF NOT EXISTS transfers (
    id BIGSERIAL PRIMARY KEY,
    sender_wallet_id BIGINT NOT NULL,
    recipient_wallet_id BIGINT NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transfers_sender_wallet FOREIGN KEY (sender_wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfers_recipient_wallet FOREIGN KEY (recipient_wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT,
    CONSTRAINT uk_transfers_sender_idempotency UNIQUE (sender_wallet_id, idempotency_key),
    CONSTRAINT chk_transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transfers_distinct_wallets CHECK (sender_wallet_id <> recipient_wallet_id)
);

CREATE INDEX IF NOT EXISTS idx_transfers_sender_idempotency ON transfers(sender_wallet_id, idempotency_key);
CREATE INDEX IF NOT EXISTS idx_transfers_sender_wallet ON transfers(sender_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transfers_recipient_wallet ON transfers(recipient_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transfers_status ON transfers(status);
CREATE INDEX IF NOT EXISTS idx_transfers_created_at ON transfers(created_at);

-- 4. Ledger Entries Table
CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ledger_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX IF NOT EXISTS idx_ledger_transfer_id ON ledger_entries(transfer_id);
CREATE INDEX IF NOT EXISTS idx_ledger_wallet_id ON ledger_entries(wallet_id);
CREATE INDEX IF NOT EXISTS idx_ledger_created_at ON ledger_entries(created_at);
