package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.btcassistant.event.EventTestSupport.BASE_TIME;
import static com.btcassistant.event.EventTestSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class LocalLowApproachRuleTest {

    private final LocalLowApproachRule rule = new LocalLowApproachRule(new EventProperties());
    private static final FeatureHistory NO_HISTORY = new FeatureHistory(java.util.List.of());

    @Test
    void triggersWhenCloseToRecentLow() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_LOW_1H, "0.005"));

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isPresent();
    }

    @Test
    void doesNotTriggerWhenFarFromRecentLow() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_LOW_1H, "0.05"));

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isEmpty();
    }
}
