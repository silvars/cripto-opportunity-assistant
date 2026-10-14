package com.btcassistant.decision.web;

import com.btcassistant.decision.Decision;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterDecisionRequest(
    @NotNull UUID alertId,
    @NotNull Decision decision
) {
}
