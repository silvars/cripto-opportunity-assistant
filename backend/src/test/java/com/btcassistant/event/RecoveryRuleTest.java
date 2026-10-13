package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static com.btcassistant.event.EventTestSupport.BASE_TIME;
import static com.btcassistant.event.EventTestSupport.historyWith;
import static com.btcassistant.event.EventTestSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class RecoveryRuleTest {

    private final RecoveryRule rule = new RecoveryRule(new EventProperties());

    @Test
    void triggersWhenPreviousWasNearLowAndCurrentBouncedBack() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_LOW_1H, "0.005"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(FeatureNames.RETURN_15M, "0.02"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isPresent();
    }

    @Test
    void doesNotTriggerWhenPreviousWasNotNearLow() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_LOW_1H, "0.05"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(FeatureNames.RETURN_15M, "0.02"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }

    @Test
    void doesNotTriggerWhenBounceIsTooSmall() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_LOW_1H, "0.005"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(FeatureNames.RETURN_15M, "0.005"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }
}
