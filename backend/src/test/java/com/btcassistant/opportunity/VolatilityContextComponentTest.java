package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureNames;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.btcassistant.opportunity.OpportunityTestSupport.event;
import static com.btcassistant.opportunity.OpportunityTestSupport.features;
import static org.assertj.core.api.Assertions.assertThat;

class VolatilityContextComponentTest {

    private final VolatilityContextComponent component = new VolatilityContextComponent(new OpportunityProperties());

    @Test
    void highShortTermVolatilityGivesFullContext() {
        var result = component.calculate(Direction.NEUTRAL, event(EventTypes.VOLATILITY_EXPANSION, "2.0"),
                features(Map.of(FeatureNames.VOLATILITY_15M, "0.02", FeatureNames.VOLATILITY_4H, "0.01")));

        assertThat(result.orElseThrow()).isEqualByComparingTo("1");
    }

    @Test
    void returnsUnknownWhenLongTermVolatilityIsZero() {
        var result = component.calculate(Direction.NEUTRAL, event(EventTypes.VOLATILITY_EXPANSION, "2.0"),
                features(Map.of(FeatureNames.VOLATILITY_15M, "0.02", FeatureNames.VOLATILITY_4H, "0")));

        assertThat(result).isEmpty();
    }

    @Test
    void returnsUnknownWhenMissing() {
        assertThat(component.calculate(Direction.NEUTRAL, event(EventTypes.VOLATILITY_EXPANSION, "2.0"), features(Map.of()))).isEmpty();
    }
}
