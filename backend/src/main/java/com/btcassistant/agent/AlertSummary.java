package com.btcassistant.agent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Resumo leve de um alerta recente para contexto do agente (SDD §36 — "recentAlerts"). */
public record AlertSummary(UUID alertId, String direction, String classification, BigDecimal score, Instant createdAt) {
}
