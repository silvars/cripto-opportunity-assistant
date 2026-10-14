package com.btcassistant.historical.web;

import java.math.BigDecimal;
import java.time.Duration;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record OutcomeStatsResponse(
    BigDecimal targetBeforeStopProbability,
    Duration medianTimeToTarget,
    BigDecimal medianAdverseMove
) {
}
