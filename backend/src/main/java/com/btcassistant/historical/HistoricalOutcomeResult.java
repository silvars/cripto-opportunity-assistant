package com.btcassistant.historical;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Resultado de {@link HistoricalOutcomeCalculator} antes de virar {@link HistoricalOutcomeEntity} (SDD §30). */
public record HistoricalOutcomeResult(
    UUID eventId,
    String horizon,
    BigDecimal futureReturn,
    BigDecimal mfe,
    BigDecimal mae,
    Duration timeToTarget,
    Duration timeToStop,
    Boolean targetBeforeStop,
    Instant evaluationEnd,
    String targetDefinitionVersion,
    String stopDefinitionVersion
) {
}
