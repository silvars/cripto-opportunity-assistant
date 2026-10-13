CREATE TABLE market_events (
    event_id UUID PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    price NUMERIC(20,8) NOT NULL,
    features_snapshot JSONB NOT NULL,
    magnitude NUMERIC(10,6),
    duration_seconds BIGINT,
    engine_version VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_symbol_time ON market_events (symbol, event_time DESC);
CREATE INDEX idx_events_type ON market_events (event_type);
