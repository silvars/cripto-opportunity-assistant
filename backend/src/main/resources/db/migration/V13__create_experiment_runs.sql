-- Fase 7 — Research/Backtesting (SDD §64, 68). Resultado completo fica em JSONB (schema evolui
-- sem migration) — colunas tipadas só para o que é usado em filtros/joins (versões, período).
CREATE TABLE experiment_runs (
    experiment_run_id UUID PRIMARY KEY,
    experiment_code VARCHAR(20) NOT NULL,
    engine_version VARCHAR(10) NOT NULL,
    feature_version VARCHAR(10) NOT NULL,
    config_version VARCHAR(10) NOT NULL,
    historical_version VARCHAR(10) NOT NULL,
    target_definition_version VARCHAR(10) NOT NULL,
    stop_definition_version VARCHAR(10) NOT NULL,
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'BACKTEST',
    dataset_from TIMESTAMPTZ NOT NULL,
    dataset_to TIMESTAMPTZ NOT NULL,
    is_out_of_sample BOOLEAN NOT NULL DEFAULT false,
    results JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_experiment_runs_experiment_code ON experiment_runs (experiment_code);
