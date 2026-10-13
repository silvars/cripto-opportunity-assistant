package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.btcassistant.event.EventTestSupport.BASE_TIME;
import static com.btcassistant.event.EventTestSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class LocalHighApproachRuleTest {

    private final LocalHighApproachRule rule = new LocalHighApproachRule(new EventProperties());
    private static final FeatureHistory NO_HISTORY = new FeatureHistory(java.util.List.of());

    @Test
    void triggersWhenCloseToRecentHigh() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_1H, "-0.005"));

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isPresent();
    }

    @Test
    void doesNotTriggerWhenFarFromRecentHigh() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_1H, "-0.05"));

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isEmpty();
    }
}
