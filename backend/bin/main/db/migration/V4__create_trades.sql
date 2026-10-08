-- V4: Trades table (immutable execution records)
CREATE TABLE trades (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    buy_order_id  UUID           NOT NULL REFERENCES orders(id),
    sell_order_id UUID           NOT NULL REFERENCES orders(id),
    symbol        VARCHAR(20)    NOT NULL,
    price         DECIMAL(24, 8) NOT NULL,
    quantity      DECIMAL(24, 8) NOT NULL,
    executed_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_trade_price_positive    CHECK (price > 0),
    CONSTRAINT chk_trade_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_trades_buy_order  ON trades (buy_order_id);
CREATE INDEX idx_trades_sell_order ON trades (sell_order_id);
CREATE INDEX idx_trades_symbol     ON trades (symbol);
CREATE INDEX idx_trades_executed   ON trades (executed_at);
