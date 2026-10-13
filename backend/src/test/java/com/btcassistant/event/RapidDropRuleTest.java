package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.btcassistant.event.EventTestSupport.BASE_TIME;
import static com.btcassistant.event.EventTestSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class RapidDropRuleTest {

    private final RapidDropRule rule = new RapidDropRule(new EventProperties());
    private static final FeatureHistory NO_HISTORY = new FeatureHistory(java.util.List.of());

    @Test
    void triggersWhenReturn15mAtOrBelowThreshold() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.RETURN_15M, "-0.03"));

        var result = rule.evaluate(new BigDecimal("100"), features, NO_HISTORY);

        assertThat(result).isPresent();
        assertThat(result.get().eventType()).isEqualTo(EventTypes.RAPID_DROP);
        assertThat(result.get().magnitude()).isEqualByComparingTo("0.03");
        assertThat(result.get().price()).isEqualByComparingTo("100");
    }

    @Test
    void doesNotTriggerWhenReturnAboveThreshold() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.RETURN_15M, "-0.01"));

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isEmpty();
    }

    @Test
    void doesNotTriggerWhenReturnMissing() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of());

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isEmpty();
    }
}
