package com.btcassistant.historical;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalFeatureNormalizerTest {

    private final HistoricalFeatureNormalizer normalizer = new HistoricalFeatureNormalizer();
    private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");

    private static DetectedEvent event(String magnitude, Duration duration, String price) {
        return new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, TIME, new BigDecimal(price),
                Map.of(), new BigDecimal(magnitude), duration, "1.0");
    }

    private static FeatureSnapshot features(Map<String, String> values) {
        Map<String, BigDecimal> decimals = new HashMap<>();
        values.forEach((k, v) -> decimals.put(k, new BigDecimal(v)));
        return new FeatureSnapshot("BTCUSDT", "1m", TIME, "1.0", decimals);
    }

    @Test
    void normalizesAllSixDimensionsWhenAvailable() {
        DetectedEvent event = event("0.05", Duration.ofMinutes(15), "100");
        FeatureSnapshot snapshot = features(Map.of(
                FeatureNames.VOLUME_RATIO, "1.5",
                FeatureNames.VOLATILITY_15M, "0.02",
                FeatureNames.MOMENTUM_15M, "5",
                FeatureNames.DISTANCE_RECENT_HIGH_1H, "-0.03",
                FeatureNames.DISTANCE_RECENT_LOW_1H, "0.08"));

        Map<String, BigDecimal> result = normalizer.normalize(event, snapshot);

        assertThat(result.get(HistoricalFeatureNames.MAGNITUDE)).isEqualByComparingTo("0.05");
        assertThat(result.get(HistoricalFeatureNames.DURATION)).isEqualByComparingTo("15");
        assertThat(result.get(HistoricalFeatureNames.VOLUME_RATIO)).isEqualByComparingTo("1.5");
        assertThat(result.get(HistoricalFeatureNames.VOLATILITY)).isEqualByComparingTo("0.02");
        // momentum absoluto (5) dividido pelo preço do evento (100) -> percentual (0.05)
        assertThat(result.get(HistoricalFeatureNames.MOMENTUM)).isEqualByComparingTo("0.05");
        // menor distância absoluta entre máxima (0.03) e mínima (0.08) -> 0.03
        assertThat(result.get(HistoricalFeatureNames.DISTANCE_TO_EXTREME)).isEqualByComparingTo("0.03");
    }

    @Test
    void omitsDimensionsWhenUnderlyingFeatureIsMissing() {
        DetectedEvent event = event("0.05", Duration.ofMinutes(15), "100");
        FeatureSnapshot snapshot = features(Map.of());

        Map<String, BigDecimal> result = normalizer.normalize(event, snapshot);

        assertThat(result).containsOnlyKeys(HistoricalFeatureNames.MAGNITUDE, HistoricalFeatureNames.DURATION);
    }
}
