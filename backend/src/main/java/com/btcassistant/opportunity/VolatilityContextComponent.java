package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

/** Volatilidade de curto prazo bem acima da base de 4h indica um contexto mais "eventful" (SDD Fase 3 §11). */
@Component
public class VolatilityContextComponent implements ScoreComponentCalculator {

    private final OpportunityProperties properties;

    public VolatilityContextComponent(OpportunityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String componentName() {
        return "volatilityContext";
    }

    @Override
    public BigDecimal weight(OpportunityProperties.Scoring scoring) {
        return scoring.getVolatilityContext();
    }

    @Override
    public Optional<BigDecimal> calculate(Direction direction, DetectedEvent event, FeatureSnapshot features) {
        BigDecimal shortTerm = features.values().get(FeatureNames.VOLATILITY_15M);
        BigDecimal longTerm = features.values().get(FeatureNames.VOLATILITY_4H);
        if (shortTerm == null || longTerm == null || longTerm.signum() == 0) {
            return Optional.empty();
        }

        BigDecimal ratio = shortTerm.divide(longTerm, MathContext.DECIMAL64);
        BigDecimal scale = properties.getEventStrengthScales().getVolatilityContext();
        BigDecimal normalized = ratio.divide(scale, MathContext.DECIMAL64).max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return Optional.of(normalized);
    }
}
