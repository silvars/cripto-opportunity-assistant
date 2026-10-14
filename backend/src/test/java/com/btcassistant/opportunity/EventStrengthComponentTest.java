package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.btcassistant.opportunity.OpportunityTestSupport.event;
import static com.btcassistant.opportunity.OpportunityTestSupport.features;
import static org.assertj.core.api.Assertions.assertThat;

class EventStrengthComponentTest {

    private final EventStrengthComponent component = new EventStrengthComponent(new OpportunityProperties());

    @Test
    void shockEventAtOrAboveScaleIsFullStrength() {
        var result = component.calculate(Direction.SELL, event(EventTypes.RAPID_DROP, "0.03"), features(Map.of()), Optional.empty());

        assertThat(result).hasValue(java.math.BigDecimal.ONE);
    }

    @Test
    void shockEventBelowScaleIsPartialStrength() {
        var result = component.calculate(Direction.SELL, event(EventTypes.RAPID_DROP, "0.015"), features(Map.of()), Optional.empty());

        assertThat(result.orElseThrow()).isEqualByComparingTo("0.5");
    }

    @Test
    void proximityEventInvertsNormalization() {
        // distância 0 (bem na mínima) deveria dar força máxima
        var atExtreme = component.calculate(Direction.NEUTRAL, event(EventTypes.LOCAL_LOW_APPROACH, "0"), features(Map.of()), Optional.empty());
        var atScaleEdge = component.calculate(Direction.NEUTRAL, event(EventTypes.LOCAL_LOW_APPROACH, "0.01"), features(Map.of()), Optional.empty());

        assertThat(atExtreme).hasValue(java.math.BigDecimal.ONE);
        assertThat(atScaleEdge.orElseThrow()).isEqualByComparingTo("0");
    }

    @Test
    void volatilityContractionInvertsNormalization() {
        var fullContraction = component.calculate(Direction.NEUTRAL, event(EventTypes.VOLATILITY_CONTRACTION, "0"), features(Map.of()), Optional.empty());

        assertThat(fullContraction).hasValue(java.math.BigDecimal.ONE);
    }

    @Test
    void neverUnknownRegardlessOfEventType() {
        assertThat(component.calculate(Direction.NEUTRAL, event(EventTypes.VOLUME_SPIKE, "10"), features(Map.of()), Optional.empty())).isPresent();
    }
}
