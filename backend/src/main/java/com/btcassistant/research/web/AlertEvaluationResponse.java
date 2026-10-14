package com.btcassistant.research.web;

import com.btcassistant.opportunity.ExecutionMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AlertEvaluationResponse(
    UUID alertId,
    Map<String, BigDecimal> futureReturnByHorizon,
    BigDecimal mfe,
    BigDecimal mae,
    Boolean targetBeforeStop,
    ExecutionMode executionMode,
    Instant evaluatedAt
) {
}
