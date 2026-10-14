package com.btcassistant.opportunity;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.historical.HistoricalContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

/** Volume acima da média confirma a convicção do movimento (SDD Fase 3 §11). */
@Component
public class VolumeConfirmationComponent implements ScoreComponentCalculator {

    private final OpportunityProperties properties;

    public VolumeConfirmationComponent(OpportunityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String componentName() {
        return "volumeConfirmation";
    }

    @Override
    public BigDecimal weight(OpportunityProperties.Scoring scoring) {
        return scoring.getVolumeConfirmation();
    }

    @Override
    public Optional<BigDecimal> calculate(Direction direction, DetectedEvent event, FeatureSnapshot features, Optional<HistoricalContext> historicalContext) {
        BigDecimal volumeZscore = features.values().get(FeatureNames.VOLUME_ZSCORE);
        if (volumeZscore == null) {
            return Optional.empty();
        }

        BigDecimal scale = properties.getEventStrengthScales().getVolumeConfirmation();
        BigDecimal normalized = volumeZscore.divide(scale, MathContext.DECIMAL64).max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return Optional.of(normalized);
    }
}
