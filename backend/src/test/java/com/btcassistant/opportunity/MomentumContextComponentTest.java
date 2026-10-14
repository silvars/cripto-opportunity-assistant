package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureNames;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.btcassistant.opportunity.OpportunityTestSupport.event;
import static com.btcassistant.opportunity.OpportunityTestSupport.features;
import static org.assertj.core.api.Assertions.assertThat;

class MomentumContextComponentTest {

    private final MomentumContextComponent component = new MomentumContextComponent(new OpportunityProperties());

    @Test
    void positiveReturnSupportsBuyDirection() {
        var result = component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.03"),
                features(Map.of(FeatureNames.RETURN_15M, "0.02")));

        assertThat(result.orElseThrow()).isEqualByComparingTo("1");
    }

    @Test
    void positiveReturnAgainstSellDirectionContributesNothing() {
        var result = component.calculate(Direction.SELL, event(EventTypes.RAPID_DROP, "0.03"),
                features(Map.of(FeatureNames.RETURN_15M, "0.02")));

        assertThat(result.orElseThrow()).isEqualByComparingTo("0");
    }

    @Test
    void neutralDirectionUsesAbsoluteValue() {
        var result = component.calculate(Direction.NEUTRAL, event(EventTypes.VOLUME_SPIKE, "3"),
                features(Map.of(FeatureNames.RETURN_15M, "-0.02")));

        assertThat(result.orElseThrow()).isEqualByComparingTo("1");
    }

    @Test
    void returnsUnknownWhenReturnMissing() {
        assertThat(component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.03"), features(Map.of()))).isEmpty();
    }
}
