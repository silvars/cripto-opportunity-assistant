-- Fase 7 — Research/Backtesting (SDD §58). Preenchida incrementalmente: cada coluna só recebe
-- valor quando o horizonte correspondente já decorreu de verdade (nunca antecipado) — por isso,
-- diferente de market_events/opportunities/alerts, esta tabela permite UPDATE (preenchimento
-- progressivo de colunas ainda NULL; uma coluna já preenchida nunca é sobrescrita).
CREATE TABLE alert_evaluations (
    alert_id UUID PRIMARY KEY REFERENCES alerts(alert_id),
    future_5m NUMERIC(10,6),
    future_15m NUMERIC(10,6),
    future_30m NUMERIC(10,6),
    future_1h NUMERIC(10,6),
    future_4h NUMERIC(10,6),
    future_12h NUMERIC(10,6),
    future_24h NUMERIC(10,6),
    mfe NUMERIC(10,6),
    mae NUMERIC(10,6),
    target_before_stop BOOLEAN,
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'LIVE',
    evaluated_at TIMESTAMPTZ NOT NULL
);
