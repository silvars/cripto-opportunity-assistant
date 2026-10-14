CREATE TABLE agent_analyses (
    analysis_id UUID PRIMARY KEY,
    alert_id UUID NOT NULL REFERENCES alerts(alert_id),
    analysis_version INT NOT NULL DEFAULT 1,
    classification VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    score NUMERIC(5,2) NOT NULL,
    confidence VARCHAR(10) NOT NULL,
    summary TEXT NOT NULL,
    evidence JSONB NOT NULL,
    historical_context JSONB,
    risk JSONB NOT NULL,
    invalidations JSONB,
    agent_version VARCHAR(10) NOT NULL,
    prompt_version VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_alert_analysis_version UNIQUE (alert_id, analysis_version)
);

CREATE INDEX idx_agent_analyses_alert ON agent_analyses (alert_id);
