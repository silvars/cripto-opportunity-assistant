package com.btcassistant.feature;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Snapshot de features calculadas para um candle de referência (SDD Fase 1 §4).
 * Valores ausentes (histórico insuficiente) são {@code null} — nunca estimados (SDD §copilot-instructions "LLM never computes metrics").
 */
public record FeatureSnapshot(
    String symbol,
    String interval,
    Instant candleOpenTime,
    String featureVersion,
    Map<String, BigDecimal> values
) {
}
