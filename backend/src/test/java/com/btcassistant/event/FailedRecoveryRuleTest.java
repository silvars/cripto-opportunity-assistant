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

class FailedRecoveryRuleTest {

    private final FailedRecoveryRule rule = new FailedRecoveryRule(new EventProperties());

    @Test
    void triggersWhenRecoveryAttemptReversesBackNearLow() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.MOMENTUM_15M, "5"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(
                FeatureNames.RETURN_15M, "-0.02", FeatureNames.DISTANCE_RECENT_LOW_1H, "0.005"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isPresent();
    }

    @Test
    void doesNotTriggerWhenPreviousWasNotRecovering() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.MOMENTUM_15M, "-5"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(
                FeatureNames.RETURN_15M, "-0.02", FeatureNames.DISTANCE_RECENT_LOW_1H, "0.005"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }

    @Test
    void doesNotTriggerWhenCurrentIsNoLongerNearLow() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.MOMENTUM_15M, "5"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(
                FeatureNames.RETURN_15M, "-0.02", FeatureNames.DISTANCE_RECENT_LOW_1H, "0.05"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }
}
