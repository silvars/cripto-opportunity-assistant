package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureNames;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.btcassistant.opportunity.OpportunityTestSupport.event;
import static com.btcassistant.opportunity.OpportunityTestSupport.features;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replica o exemplo numérico exato do SDD §24 (peso disponível 80, contribuições 64, score 80).
 * Nota: o SDD afirma {@code availableEvidenceWeight = 0.80}, mas os pesos configurados somam 110
 * (20+30+15+15+10+20), não 100 — inconsistência do próprio documento. A implementação calcula
 * {@code availableWeight/totalWeight} usando o total real configurado (110), que é
 * matematicamente correto e não assume silenciosamente que os pesos somam 100 (ver
 * docs/phases/FASE-3-opportunity-risk.md §11).
 */
class OpportunityEngineImplTest {

    private final OpportunityProperties properties = new OpportunityProperties();
    private final List<ScoreComponentCalculator> calculators = List.of(
            new EventStrengthComponent(properties),
            new HistoricalSupportComponent(new com.btcassistant.historical.HistoricalProperties()),
            new MomentumContextComponent(properties),
            new VolumeConfirmationComponent(properties),
            new VolatilityContextComponent(properties),
            new RiskPenaltyComponent(properties));
    private final OpportunityEngineImpl engine = new OpportunityEngineImpl(properties, new EventDirectionMapper(), calculators);

    @Test
    void matchesSddWorkedExampleWithHistoricalSupportUnknown() {
        var detectedEvent = event(EventTypes.RAPID_RISE, "0.03");
        var featureSnapshot = features(Map.of(
                FeatureNames.RETURN_15M, "0.02",
                FeatureNames.VOLUME_ZSCORE, "3.0",
                FeatureNames.VOLATILITY_15M, "0.02",
                FeatureNames.VOLATILITY_4H, "0.01",
                FeatureNames.VOLATILITY_24H, "0.04"));

        OpportunityResult result = engine.evaluate(detectedEvent, featureSnapshot, Optional.empty());

        assertThat(result.score()).isEqualByComparingTo("80");
        assertThat(result.classification()).isEqualTo(Classification.STRONG_SETUP);
        assertThat(result.direction()).isEqualTo(Direction.BUY);
        assertThat(result.evidenceQuality()).isEqualTo(EvidenceQuality.LIMITED);
        assertThat(result.scoreComponents()).doesNotContainKey("historicalSupport");
        assertThat(result.scoreComponents()).containsOnlyKeys(
                "eventStrength", "momentumContext", "volumeConfirmation", "volatilityContext", "riskPenalty");

        BigDecimal expectedAvailableEvidenceWeight = new BigDecimal("80").divide(new BigDecimal("110"), MathContext.DECIMAL64);
        assertThat(result.availableEvidenceWeight()).isEqualByComparingTo(expectedAvailableEvidenceWeight);
    }

    @Test
    void classificationIsNotArtificiallyCappedWhenHistoricalSupportIsUnknown() {
        // mesmo cenário acima gera STRONG_SETUP (80), não WATCH — não existe regra implícita
        // que limite a classificação só porque historicalSupport está UNKNOWN (SDD §24 regra 7).
        var detectedEvent = event(EventTypes.RAPID_RISE, "0.03");
        var featureSnapshot = features(Map.of(
                FeatureNames.RETURN_15M, "0.02",
                FeatureNames.VOLUME_ZSCORE, "3.0",
                FeatureNames.VOLATILITY_15M, "0.02",
                FeatureNames.VOLATILITY_4H, "0.01",
                FeatureNames.VOLATILITY_24H, "0.04"));

        OpportunityResult result = engine.evaluate(detectedEvent, featureSnapshot, Optional.empty());

        assertThat(result.classification()).isNotEqualTo(Classification.WATCH);
        assertThat(result.classification()).isEqualTo(Classification.STRONG_SETUP);
    }

    @Test
    void evidenceQualityIsAlwaysLimitedInThisPhaseSinceHistoricalSupportNeverResolves() {
        // mesmo com TODOS os outros 5 componentes presentes, historicalSupport continua UNKNOWN
        // nesta fase (Fase 4 ainda não existe) — portanto FULL é inalcançável até lá.
        var detectedEvent = event(EventTypes.RAPID_RISE, "0.03");
        var featureSnapshot = features(Map.of(
                FeatureNames.RETURN_15M, "0.02",
                FeatureNames.VOLUME_ZSCORE, "3.0",
                FeatureNames.VOLATILITY_15M, "0.02",
                FeatureNames.VOLATILITY_4H, "0.01",
                FeatureNames.VOLATILITY_24H, "0.04"));

        OpportunityResult result = engine.evaluate(detectedEvent, featureSnapshot, Optional.empty());

        assertThat(result.evidenceQuality()).isEqualTo(EvidenceQuality.LIMITED);
    }

    @Test
    void noneIsAValidClassificationWhenScoreBelowWatchThreshold() {
        var detectedEvent = event(EventTypes.VOLUME_SPIKE, "0.1");

        OpportunityResult result = engine.evaluate(detectedEvent, features(Map.of()), Optional.empty());

        assertThat(result.classification()).isEqualTo(Classification.NONE);
    }
}
