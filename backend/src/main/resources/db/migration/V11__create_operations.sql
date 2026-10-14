-- Fase 6 — Decision Memory (SDD §53). Execução isolada (entrada ou saída).
CREATE TABLE operations (
    operation_id UUID PRIMARY KEY,
    decision_id UUID NOT NULL REFERENCES user_decisions(decision_id),
    trade_id UUID NOT NULL REFERENCES trades(trade_id),
    side VARCHAR(10) NOT NULL,
    quantity NUMERIC(20,8) NOT NULL,
    price NUMERIC(20,8) NOT NULL,
    notional NUMERIC(24,8) NOT NULL,
    fees NUMERIC(20,8) NOT NULL DEFAULT 0,
    estimated_slippage NUMERIC(20,8) NOT NULL DEFAULT 0,
    occurred_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_operations_decision_id ON operations (decision_id);
CREATE INDEX idx_operations_trade_id ON operations (trade_id);

-- Agora que operations existe, fecha o vínculo bidirecional declarado no SDD §53.
ALTER TABLE trades
    ADD CONSTRAINT fk_trades_entry_operation FOREIGN KEY (entry_operation_id) REFERENCES operations(operation_id),
    ADD CONSTRAINT fk_trades_exit_operation FOREIGN KEY (exit_operation_id) REFERENCES operations(operation_id);
