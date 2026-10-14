package com.btcassistant.alert.web;

import com.btcassistant.alert.AlertStatus;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.risk.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record AlertResponse(
    UUID alertId,
    UUID opportunityId,
    String symbol,
    Direction direction,
    BigDecimal score,
    Classification classification,
    RiskLevel riskLevel,
    BigDecimal priceAtAlert,
    AlertStatus status,
    Instant createdAt,
    Instant expiresAt
) {
}
