CREATE TABLE opportunities (
    opportunity_id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES market_events(event_id),
    symbol VARCHAR(20) NOT NULL,
    classification VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    score NUMERIC(5,2) NOT NULL,
    score_components JSONB NOT NULL,
    available_evidence_weight NUMERIC(4,3) NOT NULL,
    evidence_quality VARCHAR(10) NOT NULL,
    risk_level VARCHAR(10) NOT NULL,
    risk_reasons JSONB NOT NULL,
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'LIVE',
    config_version VARCHAR(10) NOT NULL,
    engine_version VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_opportunities_symbol_time ON opportunities (symbol, created_at DESC);
CREATE INDEX idx_opportunities_classification ON opportunities (classification);
