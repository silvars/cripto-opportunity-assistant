package com.btcassistant.event.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** DTO de API — nunca expõe a entidade JPA diretamente (SDD §70). */
public record EventResponse(
    UUID eventId,
    String symbol,
    String eventType,
    Instant eventTime,
    BigDecimal price,
    Map<String, BigDecimal> featuresSnapshot,
    BigDecimal magnitude,
    Long durationSeconds,
    String engineVersion
) {
}
