-- Fase 6 — Decision Memory (SDD §53). Agrupa a operação de entrada e a de
-- saída de uma mesma decisão — nunca trata operações relacionadas como soltas.
-- entry_operation_id/exit_operation_id ficam sem FK aqui (operations ainda não
-- existe nesta migration — ver V11, que adiciona as constraints via ALTER).
CREATE TABLE trades (
    trade_id UUID PRIMARY KEY,
    decision_id UUID NOT NULL REFERENCES user_decisions(decision_id),
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    entry_operation_id UUID,
    exit_operation_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_trades_decision_id ON trades (decision_id);
