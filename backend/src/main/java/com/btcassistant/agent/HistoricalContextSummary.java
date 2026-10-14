package com.btcassistant.agent;

import java.math.BigDecimal;

/** Resumo histórico incluído na análise — {@code null} (ausente) quando o contexto é UNKNOWN (SDD §38). */
public record HistoricalContextSummary(int sampleSize, BigDecimal targetBeforeStopProbability) {
}
