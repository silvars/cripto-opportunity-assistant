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
 * Penalidade de risco expressa como "fração sem penalidade" (1 = sem penalidade, contribui todo o
 * peso; 0 = penalidade máxima). Baseada na volatilidade de 24h — quanto maior, menor a
 * contribuição (SDD Fase 3 §11).
 */
@Component
public class RiskPenaltyComponent implements ScoreComponentCalculator {

    private final OpportunityProperties properties;

    public RiskPenaltyComponent(OpportunityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String componentName() {
        return "riskPenalty";
    }

    @Override
    public BigDecimal weight(OpportunityProperties.Scoring scoring) {
        return scoring.getRiskPenalty();
    }

    @Override
    public Optional<BigDecimal> calculate(Direction direction, DetectedEvent event, FeatureSnapshot features, Optional<HistoricalContext> historicalContext) {
        BigDecimal volatility24h = features.values().get(FeatureNames.VOLATILITY_24H);
        if (volatility24h == null) {
            return Optional.empty();
        }

        BigDecimal scale = properties.getEventStrengthScales().getRiskPenalty();
        BigDecimal penalty = volatility24h.divide(scale, MathContext.DECIMAL64).max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return Optional.of(BigDecimal.ONE.subtract(penalty));
    }
}
