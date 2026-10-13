package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.btcassistant.event.EventTestSupport.BASE_TIME;
import static com.btcassistant.event.EventTestSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class RapidRiseRuleTest {

    private final RapidRiseRule rule = new RapidRiseRule(new EventProperties());
    private static final FeatureHistory NO_HISTORY = new FeatureHistory(java.util.List.of());

    @Test
    void triggersWhenReturn15mAtOrAboveThreshold() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.RETURN_15M, "0.03"));

        var result = rule.evaluate(new BigDecimal("100"), features, NO_HISTORY);

        assertThat(result).isPresent();
        assertThat(result.get().eventType()).isEqualTo(EventTypes.RAPID_RISE);
        assertThat(result.get().magnitude()).isEqualByComparingTo("0.03");
    }

    @Test
    void doesNotTriggerWhenReturnBelowThreshold() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.RETURN_15M, "0.01"));

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isEmpty();
    }
}
