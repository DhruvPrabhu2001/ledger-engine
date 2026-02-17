-- ===================================================================
-- Ledger-Based Banking Engine — Initial Schema
-- ===================================================================
-- Rules:
--   • Ledger entries are IMMUTABLE (no UPDATE, no DELETE)
--   • Balances are DERIVED from ledger entries (never stored)
--   • Money is never created or destroyed (sum of all entries = 0)
--   • Every transaction has equal debits and credits
-- ===================================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -------------------------------------------------------------------
-- ACCOUNT
-- -------------------------------------------------------------------
CREATE TABLE account (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

-- -------------------------------------------------------------------
-- TRANSACTION
-- -------------------------------------------------------------------
CREATE TABLE transaction (
    transaction_id  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'INITIATED',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_transaction_status CHECK (status IN ('INITIATED', 'COMPLETED', 'FAILED'))
);

-- Unique index for idempotency enforcement
CREATE UNIQUE INDEX idx_transaction_idempotency_key ON transaction (idempotency_key);

-- -------------------------------------------------------------------
-- LEDGER ENTRY
-- -------------------------------------------------------------------
CREATE TABLE ledger_entry (
    ledger_entry_id UUID   PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID   NOT NULL,
    account_id      UUID   NOT NULL,
    amount          BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Foreign keys (no cascading deletes — entries are immutable)
    CONSTRAINT fk_ledger_transaction FOREIGN KEY (transaction_id)
        REFERENCES transaction (transaction_id) ON DELETE RESTRICT,
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id)
        REFERENCES account (account_id) ON DELETE RESTRICT
);

-- Index for fast balance derivation and history queries
CREATE INDEX idx_ledger_entry_account_id ON ledger_entry (account_id);
CREATE INDEX idx_ledger_entry_transaction_id ON ledger_entry (transaction_id);
