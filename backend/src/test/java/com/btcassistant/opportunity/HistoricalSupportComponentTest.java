package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.btcassistant.opportunity.OpportunityTestSupport.event;
import static com.btcassistant.opportunity.OpportunityTestSupport.features;
import static org.assertj.core.api.Assertions.assertThat;

class HistoricalSupportComponentTest {

    private final HistoricalSupportComponent component = new HistoricalSupportComponent();

    @Test
    void alwaysUnknownUntilHistoricalAnalyzerExists() {
        assertThat(component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.05"), features(Map.of()))).isEmpty();
    }

    @Test
    void weightComesFromConfig() {
        OpportunityProperties properties = new OpportunityProperties();
        assertThat(component.weight(properties.getScoring())).isEqualByComparingTo("30");
    }
}
