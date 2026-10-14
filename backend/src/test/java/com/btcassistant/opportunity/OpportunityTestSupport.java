package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureSnapshot;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Constrói {@link DetectedEvent}/{@link FeatureSnapshot} sintéticos para testes de opportunity/risk. */
final class OpportunityTestSupport {

    static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private OpportunityTestSupport() {
    }

    static DetectedEvent event(String eventType, String magnitude) {
        return new DetectedEvent("BTCUSDT", eventType, EVENT_TIME, new BigDecimal("100"),
                Map.of(), new BigDecimal(magnitude), Duration.ofMinutes(15), "1.0");
    }

    static FeatureSnapshot features(Map<String, String> values) {
        Map<String, BigDecimal> decimals = new HashMap<>();
        values.forEach((key, value) -> decimals.put(key, new BigDecimal(value)));
        return new FeatureSnapshot("BTCUSDT", "1m", EVENT_TIME, "1.0", decimals);
    }
}
