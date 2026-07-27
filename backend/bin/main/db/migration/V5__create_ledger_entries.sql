-- V5: Ledger entries table (double-entry accounting — immutable)
-- Every financial event creates at least two rows that sum to zero per trade.
CREATE TABLE ledger_entries (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id   UUID           NOT NULL REFERENCES trades(id),
    wallet_id  UUID           NOT NULL REFERENCES wallets(id),
    entry_type VARCHAR(10)    NOT NULL,   -- DEBIT | CREDIT
    currency   VARCHAR(10)    NOT NULL,
    amount     DECIMAL(24, 8) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ledger_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_ledger_trade_id  ON ledger_entries (trade_id);
CREATE INDEX idx_ledger_wallet_id ON ledger_entries (wallet_id);

-- Constraint: ledger entries are insert-only (enforced at the service layer)
