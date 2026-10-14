package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import com.btcassistant.historical.HistoricalContext;
import com.btcassistant.historical.HistoricalProperties;
import com.btcassistant.historical.OutcomeStats;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.btcassistant.opportunity.OpportunityTestSupport.event;
import static com.btcassistant.opportunity.OpportunityTestSupport.features;
import static org.assertj.core.api.Assertions.assertThat;

class HistoricalSupportComponentTest {

    private final HistoricalSupportComponent component = new HistoricalSupportComponent(new HistoricalProperties());

    @Test
    void unknownWhenNoHistoricalContextProvided() {
        assertThat(component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.05"), features(Map.of()), Optional.empty())).isEmpty();
    }

    @Test
    void usesCanonicalHorizonProbabilityDirectly() {
        OutcomeStats oneHourStats = new OutcomeStats(new BigDecimal("0.7"), Duration.ofMinutes(20), new BigDecimal("-0.01"));
        HistoricalContext context = new HistoricalContext(40, List.of(), Map.of("1h", oneHourStats), "1.0");

        var result = component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.05"), features(Map.of()), Optional.of(context));

        assertThat(result.orElseThrow()).isEqualByComparingTo("0.7");
    }

    @Test
    void fallsBackToAverageWhenCanonicalHorizonMissing() {
        OutcomeStats fifteenMin = new OutcomeStats(new BigDecimal("0.4"), Duration.ofMinutes(5), new BigDecimal("-0.01"));
        OutcomeStats fourHour = new OutcomeStats(new BigDecimal("0.6"), Duration.ofMinutes(90), new BigDecimal("-0.02"));
        HistoricalContext context = new HistoricalContext(40, List.of(), Map.of("15m", fifteenMin, "4h", fourHour), "1.0");

        var result = component.calculate(Direction.BUY, event(EventTypes.RAPID_RISE, "0.05"), features(Map.of()), Optional.of(context));

        assertThat(result.orElseThrow()).isEqualByComparingTo("0.5");
    }

    @Test
    void weightComesFromConfig() {
        OpportunityProperties properties = new OpportunityProperties();
        assertThat(component.weight(properties.getScoring())).isEqualByComparingTo("30");
    }
}
