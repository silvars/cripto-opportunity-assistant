CREATE TABLE historical_outcomes (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES market_events(event_id),
    horizon VARCHAR(10) NOT NULL,
    future_return NUMERIC(10,6),
    mfe NUMERIC(10,6),
    mae NUMERIC(10,6),
    time_to_target_seconds BIGINT,
    time_to_stop_seconds BIGINT,
    target_before_stop BOOLEAN,
    evaluation_end TIMESTAMPTZ NOT NULL,
    target_definition_version VARCHAR(10) NOT NULL,
    stop_definition_version VARCHAR(10) NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_outcome UNIQUE (event_id, horizon)
);

CREATE INDEX idx_outcomes_event ON historical_outcomes (event_id);
