package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.historical.HistoricalContext;
import com.btcassistant.historical.HistoricalProperties;
import com.btcassistant.historical.OutcomeStats;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

/**
 * Usa {@code targetBeforeStopProbability} do horizonte canônico configurado como valor
 * normalizado (já nasce em [0,1], sem transformação adicional). UNKNOWN sempre que não houver
 * {@link HistoricalContext} (amostra insuficiente, SDD §24/§29) ou nenhum horizonte disponível.
 */
@Component
public class HistoricalSupportComponent implements ScoreComponentCalculator {

    private final HistoricalProperties properties;

    public HistoricalSupportComponent(HistoricalProperties properties) {
        this.properties = properties;
    }

    @Override
    public String componentName() {
        return "historicalSupport";
    }

    @Override
    public BigDecimal weight(OpportunityProperties.Scoring scoring) {
        return scoring.getHistoricalSupport();
    }

    @Override
    public Optional<BigDecimal> calculate(Direction direction, DetectedEvent event, FeatureSnapshot features, Optional<HistoricalContext> historicalContext) {
        if (historicalContext.isEmpty() || historicalContext.get().outcomesByHorizon().isEmpty()) {
            return Optional.empty();
        }

        var outcomesByHorizon = historicalContext.get().outcomesByHorizon();
        OutcomeStats canonical = outcomesByHorizon.get(properties.getCanonicalHorizon());
        if (canonical != null) {
            return Optional.of(canonical.targetBeforeStopProbability());
        }

        // horizonte canônico sem dados suficientes nesta avaliação — usa a média dos disponíveis
        BigDecimal sum = BigDecimal.ZERO;
        for (OutcomeStats stats : outcomesByHorizon.values()) {
            sum = sum.add(stats.targetBeforeStopProbability());
        }
        return Optional.of(sum.divide(BigDecimal.valueOf(outcomesByHorizon.size()), MathContext.DECIMAL64));
    }
}
