package com.btcassistant.historical;

import java.math.BigDecimal;
import java.time.Duration;

/** Estatística agregada de outcomes para um horizonte específico (SDD Fase 4 §4). */
public record OutcomeStats(
    BigDecimal targetBeforeStopProbability,
    Duration medianTimeToTarget,
    BigDecimal medianAdverseMove
) {
}
