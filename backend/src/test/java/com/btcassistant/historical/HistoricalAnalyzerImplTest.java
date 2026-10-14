package com.btcassistant.historical;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.MarketFeatureEntity;
import com.btcassistant.feature.MarketFeatureRepository;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalAnalyzerImplTest {

    private static final Instant AS_OF = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instrument INSTRUMENT = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);

    @Mock
    private HistoricalFeatureStoreRepository featureStoreRepository;
    @Mock
    private HistoricalOutcomeRepository outcomeRepository;
    @Mock
    private MarketProperties marketProperties;
    @Mock
    private MarketFeatureRepository marketFeatureRepository;

    private final HistoricalFeatureNormalizer normalizer = new HistoricalFeatureNormalizer();
    private final SimilarityCalculator similarityCalculator = new SimilarityCalculator();
    private final FeatureProperties featureProperties = new FeatureProperties();

    private HistoricalAnalyzerImpl analyzer(HistoricalProperties properties) {
        return new HistoricalAnalyzerImpl(featureStoreRepository, outcomeRepository, normalizer, similarityCalculator,
                properties, marketProperties, marketFeatureRepository, featureProperties);
    }

    private static DetectedEvent event() {
        return new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, AS_OF, new BigDecimal("100"),
                Map.of(), new BigDecimal("0.05"), Duration.ofMinutes(15), "1.0");
    }

    private static MarketFeatureEntity currentFeatureEntity() {
        return MarketFeatureEntity.from(snapshot(Map.of(FeatureNames.VOLUME_RATIO, "1.1")));
    }

    private static com.btcassistant.feature.FeatureSnapshot snapshot(Map<String, String> values) {
        Map<String, BigDecimal> decimals = new HashMap<>();
        values.forEach((k, v) -> decimals.put(k, new BigDecimal(v)));
        return new com.btcassistant.feature.FeatureSnapshot("BTCUSDT", "1m", AS_OF, "1.0", decimals);
    }

    private static HistoricalFeatureStoreEntity candidate(Instant eventTime, String volumeRatio) {
        return HistoricalFeatureStoreEntity.create("BTCUSDT", java.util.UUID.randomUUID(), EventTypes.RAPID_DROP,
                eventTime, Map.of(HistoricalFeatureNames.VOLUME_RATIO, new BigDecimal(volumeRatio)), "1.0");
    }

    @Test
    void returnsEmptyContextWhenCurrentFeatureSnapshotMissing() {
        when(marketProperties.findEnabledBySymbol("BTCUSDT")).thenReturn(INSTRUMENT);
        when(marketFeatureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        HistoricalContext context = analyzer(new HistoricalProperties()).findSimilar(event(), AS_OF);

        assertThat(context.sampleSize()).isZero();
        assertThat(context.similarEvents()).isEmpty();
    }

    @Test
    void returnsEmptyContextWhenSampleBelowMinimum() {
        when(marketProperties.findEnabledBySymbol("BTCUSDT")).thenReturn(INSTRUMENT);
        when(marketFeatureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(any(), any(), any(), any()))
                .thenReturn(Optional.of(currentFeatureEntity()));
        when(featureStoreRepository.findCandidates(any(), any(), any())).thenReturn(List.of(candidate(AS_OF.minusSeconds(60), "1.1")));

        HistoricalProperties properties = new HistoricalProperties();
        properties.getSimilarity().setMinSampleSize(5);

        HistoricalContext context = analyzer(properties).findSimilar(event(), AS_OF);

        assertThat(context.sampleSize()).isZero();
    }

    @Test
    void returnsSampleSizeAndSortsByMostSimilarWhenAboveMinimum() {
        when(marketProperties.findEnabledBySymbol("BTCUSDT")).thenReturn(INSTRUMENT);
        when(marketFeatureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(any(), any(), any(), any()))
                .thenReturn(Optional.of(currentFeatureEntity()));
        List<HistoricalFeatureStoreEntity> candidates = List.of(
                candidate(AS_OF.minusSeconds(60), "5.0"),
                candidate(AS_OF.minusSeconds(120), "1.1"),
                candidate(AS_OF.minusSeconds(180), "1.2"));
        when(featureStoreRepository.findCandidates(any(), any(), any())).thenReturn(candidates);
        when(outcomeRepository.findKnownAsOf(any(), any(), any())).thenReturn(List.of());

        HistoricalProperties properties = new HistoricalProperties();
        properties.getSimilarity().setMinSampleSize(2);

        HistoricalContext context = analyzer(properties).findSimilar(event(), AS_OF);

        assertThat(context.sampleSize()).isEqualTo(3);
        // o candidato com volumeRatio=1.1 (mais perto do atual, 1.1) deve vir primeiro
        assertThat(context.similarEvents().get(0).similarityScore())
                .isGreaterThanOrEqualTo(context.similarEvents().get(1).similarityScore());
    }
}
