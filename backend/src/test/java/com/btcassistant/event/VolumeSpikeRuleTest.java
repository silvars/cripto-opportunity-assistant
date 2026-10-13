package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.btcassistant.event.EventTestSupport.BASE_TIME;
import static com.btcassistant.event.EventTestSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class VolumeSpikeRuleTest {

    private final VolumeSpikeRule rule = new VolumeSpikeRule(new EventProperties());
    private static final FeatureHistory NO_HISTORY = new FeatureHistory(java.util.List.of());

    @Test
    void triggersWhenZscoreAtOrAboveThreshold() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.VOLUME_ZSCORE, "3.0"));

        var result = rule.evaluate(new BigDecimal("100"), features, NO_HISTORY);

        assertThat(result).isPresent();
        assertThat(result.get().magnitude()).isEqualByComparingTo("3.0");
    }

    @Test
    void doesNotTriggerWhenZscoreBelowThreshold() {
        FeatureSnapshot features = snapshot(BASE_TIME, Map.of(FeatureNames.VOLUME_ZSCORE, "1.0"));

        assertThat(rule.evaluate(new BigDecimal("100"), features, NO_HISTORY)).isEmpty();
    }
}
