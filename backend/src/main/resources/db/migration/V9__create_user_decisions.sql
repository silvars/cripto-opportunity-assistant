-- Fase 6 — Decision Memory (SDD §52, 57). Decisão do usuário é um fato
-- independente do que o sistema calculou (alert) e do que o mercado fez depois —
-- nunca fundidos na mesma linha mutável.
CREATE TABLE user_decisions (
    decision_id UUID PRIMARY KEY,
    alert_id UUID NOT NULL REFERENCES alerts(alert_id),
    decision VARCHAR(10) NOT NULL,
    execution_mode VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_user_decisions_alert_id ON user_decisions (alert_id);
