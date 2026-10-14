package com.btcassistant.risk;

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

class SimpleRiskEngineTest {

    private final SimpleRiskEngine engine = new SimpleRiskEngine(new RiskProperties());
    private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");

    private static DetectedEvent event(String magnitude) {
        return new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, TIME, new BigDecimal("100"),
                Map.of(), new BigDecimal(magnitude), Duration.ofMinutes(15), "1.0");
    }

    private static FeatureSnapshot features(Map<String, String> values) {
        Map<String, BigDecimal> decimals = new HashMap<>();
        values.forEach((k, v) -> decimals.put(k, new BigDecimal(v)));
        return new FeatureSnapshot("BTCUSDT", "1m", TIME, "1.0", decimals);
    }

    @Test
    void returnsUnknownWhenVolatility24hMissing() {
        RiskAssessment result = engine.assess(event("0.03"), features(Map.of()));

        assertThat(result.level()).isEqualTo(RiskLevel.UNKNOWN);
        assertThat(result.reasons()).containsExactly("insufficient_volatility_data");
    }

    @Test
    void lowCompositeRiskWhenAllSignalsAreCalm() {
        RiskAssessment result = engine.assess(event("0.01"), features(Map.of(
                FeatureNames.VOLATILITY_24H, "0.001",
                FeatureNames.DISTANCE_RECENT_HIGH_1H, "-0.05",
                FeatureNames.DISTANCE_RECENT_LOW_1H, "0.05")));

        assertThat(result.level()).isEqualTo(RiskLevel.LOW);
        assertThat(result.reasons()).containsExactly("no_significant_risk_signals");
    }

    @Test
    void highCompositeRiskWhenMultipleSignalsAreSignificant() {
        RiskAssessment result = engine.assess(event("0.5"), features(Map.of(
                FeatureNames.VOLATILITY_24H, "0.10",
                FeatureNames.DISTANCE_RECENT_HIGH_1H, "0",
                FeatureNames.DISTANCE_RECENT_LOW_1H, "0.5")));

        assertThat(result.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.reasons()).contains("high_volatility", "near_recent_extreme", "large_magnitude_move");
    }

    @Test
    void nearRecentExtremeIsFlaggedAsReason() {
        RiskAssessment result = engine.assess(event("0.01"), features(Map.of(
                FeatureNames.VOLATILITY_24H, "0.001",
                FeatureNames.DISTANCE_RECENT_HIGH_1H, "0",
                FeatureNames.DISTANCE_RECENT_LOW_1H, "0.05")));

        assertThat(result.reasons()).contains("near_recent_extreme");
    }
}
