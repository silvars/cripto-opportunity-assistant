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

class FailedBreakoutRuleTest {

    private final FailedBreakoutRule rule = new FailedBreakoutRule(new EventProperties());

    @Test
    void triggersWhenBreakoutReversesBackBelowHigh() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "0"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(
                FeatureNames.DISTANCE_RECENT_HIGH_24H, "-0.01", FeatureNames.RETURN_15M, "-0.01"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isPresent();
    }

    @Test
    void doesNotTriggerWhenPreviousHadNotBrokenOut() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "-0.02"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(
                FeatureNames.DISTANCE_RECENT_HIGH_24H, "-0.01", FeatureNames.RETURN_15M, "-0.01"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }

    @Test
    void doesNotTriggerWhenStillAtHigh() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "0"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(
                FeatureNames.DISTANCE_RECENT_HIGH_24H, "0", FeatureNames.RETURN_15M, "-0.01"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }
}
