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

class BreakoutRuleTest {

    private final BreakoutRule rule = new BreakoutRule(new EventProperties());

    @Test
    void triggersWhenPreviousWasBelowHighAndCurrentIsAtNewHigh() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "-0.01"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "0"));

        var result = rule.evaluate(new BigDecimal("100"), current, historyWith(previous));

        assertThat(result).isPresent();
        assertThat(result.get().magnitude()).isEqualByComparingTo("0.01");
        assertThat(result.get().duration()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void doesNotTriggerWhenPreviousWasAlreadyAtHigh() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "0"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "0"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }

    @Test
    void doesNotTriggerWhenCurrentIsNotAtHigh() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "-0.02"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "-0.01"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }

    @Test
    void doesNotTriggerWhenNoHistoryAvailable() {
        FeatureSnapshot current = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_HIGH_24H, "0"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, new FeatureHistory(java.util.List.of()))).isEmpty();
    }
}
