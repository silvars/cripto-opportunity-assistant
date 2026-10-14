package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureNames;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.btcassistant.opportunity.OpportunityTestSupport.event;
import static com.btcassistant.opportunity.OpportunityTestSupport.features;
import static org.assertj.core.api.Assertions.assertThat;

class RiskPenaltyComponentTest {

    private final RiskPenaltyComponent component = new RiskPenaltyComponent(new OpportunityProperties());

    @Test
    void lowVolatilityContributesFullWeight() {
        var result = component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.03"),
                features(Map.of(FeatureNames.VOLATILITY_24H, "0")));

        assertThat(result.orElseThrow()).isEqualByComparingTo("1");
    }

    @Test
    void highVolatilityContributesPartialWeight() {
        var result = component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.03"),
                features(Map.of(FeatureNames.VOLATILITY_24H, "0.04")));

        assertThat(result.orElseThrow()).isEqualByComparingTo("0.2");
    }

    @Test
    void returnsUnknownWhenVolatilityMissing() {
        assertThat(component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.03"), features(Map.of()))).isEmpty();
    }
}
