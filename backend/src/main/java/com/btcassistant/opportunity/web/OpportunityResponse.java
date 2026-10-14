package com.btcassistant.opportunity.web;

import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.EvidenceQuality;
import com.btcassistant.risk.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record OpportunityResponse(
    UUID opportunityId,
    UUID eventId,
    String symbol,
    Classification classification,
    Direction direction,
    BigDecimal score,
    Map<String, BigDecimal> scoreComponents,
    BigDecimal availableEvidenceWeight,
    EvidenceQuality evidenceQuality,
    RiskLevel riskLevel,
    List<String> riskReasons,
    String configVersion,
    String engineVersion,
    Instant createdAt
) {
}
