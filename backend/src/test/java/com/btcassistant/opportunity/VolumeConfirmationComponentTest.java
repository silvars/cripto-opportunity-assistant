package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureNames;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.btcassistant.opportunity.OpportunityTestSupport.event;
import static com.btcassistant.opportunity.OpportunityTestSupport.features;
import static org.assertj.core.api.Assertions.assertThat;

class VolumeConfirmationComponentTest {

    private final VolumeConfirmationComponent component = new VolumeConfirmationComponent(new OpportunityProperties());

    @Test
    void highZscoreGivesFullConfirmation() {
        var result = component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.03"),
                features(Map.of(FeatureNames.VOLUME_ZSCORE, "3.0")), Optional.empty());

        assertThat(result.orElseThrow()).isEqualByComparingTo("1");
    }

    @Test
    void belowAverageVolumeClampsToZero() {
        var result = component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.03"),
                features(Map.of(FeatureNames.VOLUME_ZSCORE, "-1.0")), Optional.empty());

        assertThat(result.orElseThrow()).isEqualByComparingTo("0");
    }

    @Test
    void returnsUnknownWhenZscoreMissing() {
        assertThat(component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.03"), features(Map.of()), Optional.empty())).isEmpty();
    }
}
