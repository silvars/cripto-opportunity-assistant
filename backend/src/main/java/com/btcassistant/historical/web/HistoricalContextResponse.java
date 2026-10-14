package com.btcassistant.historical.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record HistoricalContextResponse(
    int sampleSize,
    List<SimilarEventResponse> similarEvents,
    Map<String, OutcomeStatsResponse> outcomesByHorizon,
    String historicalVersion
) {

    public record SimilarEventResponse(UUID eventId, Instant eventTime, java.math.BigDecimal similarityScore) {
    }
}
