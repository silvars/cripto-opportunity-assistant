package com.btcassistant.agent.web;

import com.btcassistant.agent.AnalysisStatus;
import com.btcassistant.agent.Confidence;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record AgentAnalysisResponse(
    UUID analysisId,
    UUID alertId,
    int analysisVersion,
    Classification classification,
    Direction direction,
    BigDecimal score,
    Confidence confidence,
    String summary,
    List<String> evidence,
    Map<String, Object> historicalContext,
    Map<String, Object> risk,
    List<String> invalidations,
    String agentVersion,
    String promptVersion,
    AnalysisStatus status,
    Instant createdAt
) {
}
