package com.btcassistant.event;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Evento detectado por uma {@link EventRule} — imutável por natureza (nunca é atualizado após criado, SDD §21). */
public record DetectedEvent(
    String symbol,
    String eventType,
    Instant eventTime,
    BigDecimal price,
    Map<String, BigDecimal> featuresSnapshot,
    BigDecimal magnitude,
    Duration duration,
    String engineVersion
) {
}
