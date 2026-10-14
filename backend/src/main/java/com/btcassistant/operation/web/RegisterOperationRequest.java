package com.btcassistant.operation.web;

import com.btcassistant.operation.Side;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegisterOperationRequest(
    @NotNull UUID decisionId,
    @NotNull Side side,
    @NotNull @DecimalMin(value = "0.00000001") BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.00000001") BigDecimal price,
    @PositiveOrZero BigDecimal fees,
    @PositiveOrZero BigDecimal estimatedSlippage,
    @NotNull Instant occurredAt
) {
}
