package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.historical.HistoricalContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Combina os {@link ScoreComponentCalculator} configurados em um score 0-100 explicável,
 * aplicando a regra de renormalização quando algum componente está UNKNOWN (SDD §24 — ver exemplo
 * numérico: peso disponível 80/110, contribuições 64 → score 80).
 */
@Component
public class OpportunityEngineImpl implements OpportunityEngine {

    /** Versão do algoritmo de combinação de componentes — distinta de {@code configVersion} (pesos/thresholds). */
    public static final String ENGINE_VERSION = "1.0";

    private final OpportunityProperties properties;
    private final EventDirectionMapper directionMapper;
    private final List<ScoreComponentCalculator> calculators;

    public OpportunityEngineImpl(OpportunityProperties properties,
                                 EventDirectionMapper directionMapper,
                                 List<ScoreComponentCalculator> calculators) {
        this.properties = properties;
        this.directionMapper = directionMapper;
        this.calculators = calculators;
    }

    @Override
    public OpportunityResult evaluate(DetectedEvent event, FeatureSnapshot features, Optional<HistoricalContext> historicalContext) {
        Direction direction = directionMapper.map(event.eventType());
        OpportunityProperties.Scoring scoring = properties.getScoring();

        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal availableWeight = BigDecimal.ZERO;
        BigDecimal numerator = BigDecimal.ZERO;
        Map<String, BigDecimal> scoreComponents = new LinkedHashMap<>();

        for (ScoreComponentCalculator calculator : calculators) {
            BigDecimal weight = calculator.weight(scoring);
            totalWeight = totalWeight.add(weight);

            Optional<BigDecimal> normalized = calculator.calculate(direction, event, features, historicalContext);
            if (normalized.isPresent()) {
                BigDecimal contribution = weight.multiply(normalized.get());
                numerator = numerator.add(contribution);
                availableWeight = availableWeight.add(weight);
                scoreComponents.put(calculator.componentName(), contribution);
            }
        }

        BigDecimal score = availableWeight.signum() == 0
                ? BigDecimal.ZERO
                : numerator.divide(availableWeight, MathContext.DECIMAL64).multiply(BigDecimal.valueOf(100));
        BigDecimal availableEvidenceWeight = totalWeight.signum() == 0
                ? BigDecimal.ZERO
                : availableWeight.divide(totalWeight, MathContext.DECIMAL64);
        EvidenceQuality evidenceQuality = scoreComponents.size() == calculators.size()
                ? EvidenceQuality.FULL
                : EvidenceQuality.LIMITED;

        Classification classification = classify(score);

        return new OpportunityResult(direction, classification, score, Map.copyOf(scoreComponents),
                availableEvidenceWeight, evidenceQuality, properties.getConfigVersion(), ENGINE_VERSION);
    }

    private Classification classify(BigDecimal score) {
        if (score.compareTo(properties.getStrongSetupThreshold()) >= 0) {
            return Classification.STRONG_SETUP;
        }
        if (score.compareTo(properties.getSetupThreshold()) >= 0) {
            return Classification.SETUP;
        }
        if (score.compareTo(properties.getWatchThreshold()) >= 0) {
            return Classification.WATCH;
        }
        return Classification.NONE;
    }
}
