-- V2: Wallets table (one row per user per currency)
CREATE TABLE wallets (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    currency   VARCHAR(10)    NOT NULL,
    balance    DECIMAL(24, 8) NOT NULL DEFAULT 0.00000000,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wallet_user_currency UNIQUE (user_id, currency),
    CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);
