package com.btcassistant.decision.web;

import com.btcassistant.decision.Decision;
import com.btcassistant.opportunity.ExecutionMode;

import java.time.Instant;
import java.util.UUID;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record UserDecisionResponse(
    UUID decisionId,
    UUID alertId,
    Decision decision,
    ExecutionMode executionMode,
    Instant createdAt
) {
}
