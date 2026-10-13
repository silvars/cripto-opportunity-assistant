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

class BreakdownRuleTest {

    private final BreakdownRule rule = new BreakdownRule(new EventProperties());

    @Test
    void triggersWhenPreviousWasAboveLowAndCurrentIsAtNewLow() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_LOW_24H, "0.01"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(FeatureNames.DISTANCE_RECENT_LOW_24H, "0"));

        var result = rule.evaluate(new BigDecimal("100"), current, historyWith(previous));

        assertThat(result).isPresent();
        assertThat(result.get().magnitude()).isEqualByComparingTo("0.01");
    }

    @Test
    void doesNotTriggerWhenPreviousWasAlreadyAtLow() {
        FeatureSnapshot previous = snapshot(BASE_TIME, Map.of(FeatureNames.DISTANCE_RECENT_LOW_24H, "0"));
        FeatureSnapshot current = snapshot(BASE_TIME.plus(Duration.ofMinutes(1)), Map.of(FeatureNames.DISTANCE_RECENT_LOW_24H, "0"));

        assertThat(rule.evaluate(new BigDecimal("100"), current, historyWith(previous))).isEmpty();
    }
}
