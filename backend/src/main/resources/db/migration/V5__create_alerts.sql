CREATE TABLE alerts (
    alert_id UUID PRIMARY KEY,
    opportunity_id UUID NOT NULL REFERENCES opportunities(opportunity_id),
    symbol VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    score NUMERIC(5,2) NOT NULL,
    classification VARCHAR(20) NOT NULL,
    risk_level VARCHAR(10) NOT NULL,
    price_at_alert NUMERIC(20,8) NOT NULL,
    market_snapshot JSONB NOT NULL,
    feature_snapshot JSONB NOT NULL,
    engine_version VARCHAR(10) NOT NULL,
    config_version VARCHAR(10) NOT NULL,
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'LIVE',
    status VARCHAR(20) NOT NULL DEFAULT 'DETECTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_alerts_symbol_direction_status ON alerts (symbol, direction, status);
CREATE INDEX idx_alerts_symbol_time ON alerts (symbol, created_at DESC);
