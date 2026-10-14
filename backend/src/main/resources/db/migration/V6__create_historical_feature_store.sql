CREATE TABLE historical_feature_store (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    event_id UUID NOT NULL REFERENCES market_events(event_id),
    event_type VARCHAR(40) NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    normalized_features JSONB NOT NULL,
    historical_version VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_historical_feature_event UNIQUE (event_id)
);

CREATE INDEX idx_hfs_symbol_type_time ON historical_feature_store (symbol, event_type, event_time);
