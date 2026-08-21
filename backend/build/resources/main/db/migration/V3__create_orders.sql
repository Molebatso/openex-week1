-- V3: Orders table
CREATE TABLE orders (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    symbol           VARCHAR(20)    NOT NULL,
    side             VARCHAR(10)    NOT NULL,   -- BUY | SELL
    type             VARCHAR(10)    NOT NULL,   -- LIMIT | MARKET
    price            DECIMAL(24, 8),            -- NULL for MARKET orders
    quantity         DECIMAL(24, 8) NOT NULL,
    filled_quantity  DECIMAL(24, 8) NOT NULL DEFAULT 0.00000000,
    status           VARCHAR(20)    NOT NULL DEFAULT 'OPEN', -- OPEN | PARTIAL | FILLED | CANCELLED
    idempotency_key  UUID           UNIQUE,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_filled_quantity   CHECK (filled_quantity >= 0 AND filled_quantity <= quantity),
    CONSTRAINT chk_limit_price             CHECK (type = 'MARKET' OR price IS NOT NULL)
);

CREATE INDEX idx_orders_user_id    ON orders (user_id);
CREATE INDEX idx_orders_symbol     ON orders (symbol);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_idem_key   ON orders (idempotency_key);
