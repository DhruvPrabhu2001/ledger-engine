-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Create Account Table
CREATE TABLE account (
    account_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create Transaction Table
CREATE TABLE transaction (
    transaction_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, COMPLETED, FAILED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    error_reason TEXT
);

-- 3. Create Ledger Entry Table
CREATE TABLE ledger_entry (
    ledger_entry_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL REFERENCES transaction(transaction_id),
    account_id UUID NOT NULL REFERENCES account(account_id),
    amount BIGINT NOT NULL, -- Positive = Credit, Negative = Debit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transaction_fk FOREIGN KEY (transaction_id) REFERENCES transaction(transaction_id),
    CONSTRAINT account_fk FOREIGN KEY (account_id) REFERENCES account(account_id)
);

-- Indexes for performance
CREATE INDEX idx_ledger_entry_account_id ON ledger_entry(account_id);
CREATE INDEX idx_ledger_entry_transaction_id ON ledger_entry(transaction_id);
