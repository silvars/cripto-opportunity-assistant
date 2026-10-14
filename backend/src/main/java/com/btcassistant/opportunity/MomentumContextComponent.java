package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.historical.HistoricalContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

/**
 * Mede se o momentum de curto prazo (retorno de 15m) reforça a direção do evento. Para eventos
 * sem direção implícita ({@link Direction#NEUTRAL}), mede apenas a presença de momentum,
 * independente do sinal (SDD Fase 3 §11).
 */
@Component
public class MomentumContextComponent implements ScoreComponentCalculator {

    private final OpportunityProperties properties;

    public MomentumContextComponent(OpportunityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String componentName() {
        return "momentumContext";
    }

    @Override
    public BigDecimal weight(OpportunityProperties.Scoring scoring) {
        return scoring.getMomentumContext();
    }

    @Override
    public Optional<BigDecimal> calculate(Direction direction, DetectedEvent event, FeatureSnapshot features, Optional<HistoricalContext> historicalContext) {
        BigDecimal return15m = features.values().get(FeatureNames.RETURN_15M);
        if (return15m == null) {
            return Optional.empty();
        }

        BigDecimal scale = properties.getEventStrengthScales().getMomentumContext();
        BigDecimal signed = switch (direction) {
            case BUY -> return15m;
            case SELL -> return15m.negate();
            case NEUTRAL -> return15m.abs();
        };

        BigDecimal normalized = signed.divide(scale, MathContext.DECIMAL64).max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return Optional.of(normalized);
    }
}
