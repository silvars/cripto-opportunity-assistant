package com.btcassistant.research.web;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record ExperimentRunResponse(
    UUID experimentRunId,
    String experimentCode,
    String engineVersion,
    String featureVersion,
    String configVersion,
    String historicalVersion,
    String targetDefinitionVersion,
    String stopDefinitionVersion,
    String executionMode,
    Instant datasetFrom,
    Instant datasetTo,
    boolean outOfSample,
    Map<String, Object> results,
    Instant createdAt
) {
}
