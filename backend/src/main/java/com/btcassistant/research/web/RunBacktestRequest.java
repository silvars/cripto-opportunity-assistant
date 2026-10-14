package com.btcassistant.research.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RunBacktestRequest(
    @NotBlank String experimentCode,
    @NotBlank String symbol,
    @NotNull Instant from,
    @NotNull Instant to,
    @NotBlank String engineVersion,
    @NotBlank String featureVersion,
    @NotBlank String configVersion,
    @NotBlank String historicalVersion,
    @NotBlank String targetDefinitionVersion,
    @NotBlank String stopDefinitionVersion,
    boolean outOfSample
) {
}
