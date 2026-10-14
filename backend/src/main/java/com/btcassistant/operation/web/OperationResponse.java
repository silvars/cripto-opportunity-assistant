package com.btcassistant.operation.web;

import com.btcassistant.operation.Side;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record OperationResponse(
    UUID operationId,
    UUID decisionId,
    UUID tradeId,
    Side side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal notional,
    BigDecimal fees,
    BigDecimal estimatedSlippage,
    Instant occurredAt,
    String source,
    Instant createdAt
) {
}
