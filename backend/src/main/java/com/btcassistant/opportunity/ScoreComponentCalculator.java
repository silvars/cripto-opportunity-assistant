package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.historical.HistoricalContext;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Componente isolado e auditável do score (SDD §24). Retorna um valor normalizado em [0,1]
 * (1 = contribuição máxima) ou {@link Optional#empty()} quando o dado necessário está ausente —
 * nesse caso o componente é UNKNOWN e não participa do numerador (ver §4 do orquestrador).
 */
public interface ScoreComponentCalculator {

    /** Nome usado como chave em {@code scoreComponents} (SDD §24: eventStrength, historicalSupport, ...). */
    String componentName();

    BigDecimal weight(OpportunityProperties.Scoring scoring);

    Optional<BigDecimal> calculate(Direction direction, DetectedEvent event, FeatureSnapshot features, Optional<HistoricalContext> historicalContext);
}
