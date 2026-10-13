package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.btcassistant.event.EventTestSupport.BASE_TIME;
import static com.btcassistant.event.EventTestSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class VolatilityContractionRuleTest {

    private final VolatilityContractionRule rule = new VolatilityContractionRule(new EventProperties());
    private static final FeatureHistory NO_HISTORY = new FeatureHistory(java.util.List.of());

    @Test
    void triggersWhenShortTermVolatilityFarBelowLongTerm() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(
                FeatureNames.VOLATILITY_15M, "0.002", FeatureNames.VOLATILITY_4H, "0.01"));

        var result = rule.evaluate(new BigDecimal("100"), features, NO_HISTORY);

        assertThat(result).isPresent();
        assertThat(result.get().magnitude()).isEqualByComparingTo("0.2");
    }

    @Test
    void doesNotTriggerWhenRatioAboveThreshold() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(
                FeatureNames.VOLATILITY_15M, "0.009", FeatureNames.VOLATILITY_4H, "0.01"));

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isEmpty();
    }
}
