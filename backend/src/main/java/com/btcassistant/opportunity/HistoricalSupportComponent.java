package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/** Placeholder — sempre UNKNOWN até o Historical Analyzer existir (SDD Fase 3 §2 "Fora", Fase 4). */
@Component
public class HistoricalSupportComponent implements ScoreComponentCalculator {

    @Override
    public String componentName() {
        return "historicalSupport";
    }

    @Override
    public BigDecimal weight(OpportunityProperties.Scoring scoring) {
        return scoring.getHistoricalSupport();
    }

    @Override
    public Optional<BigDecimal> calculate(Direction direction, DetectedEvent event, FeatureSnapshot features) {
        return Optional.empty();
    }
}
