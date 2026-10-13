package com.btcassistant.event;

import com.btcassistant.feature.FeatureSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Constrói {@link FeatureSnapshot} sintéticos para testes de regras de evento. */
final class EventTestSupport {

    static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private EventTestSupport() {
    }

    static FeatureSnapshot snapshot(Instant candleOpenTime, Map<String, String> values) {
        Map<String, BigDecimal> decimals = new HashMap<>();
        values.forEach((key, value) -> decimals.put(key, new BigDecimal(value)));
        return new FeatureSnapshot("BTCUSDT", "1m", candleOpenTime, "1.0", decimals);
    }

    static FeatureHistory historyWith(FeatureSnapshot... snapshots) {
        return new FeatureHistory(java.util.List.of(snapshots));
    }
}
