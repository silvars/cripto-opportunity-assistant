package com.btcassistant.historical;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.feature.MarketFeatureRepository;
import com.btcassistant.market.MarketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do Historical Analyzer (SDD Fase 4 §4/§6). A exclusão do próprio evento (SDD §31:
 * {@code eventId != currentEvent.eventId}) é garantida estruturalmente pelo filtro estrito
 * {@code eventTime < asOf}: como {@code asOf} é sempre o {@code eventTime} do evento avaliado,
 * nenhuma linha com o mesmo {@code eventTime} (incluindo a do próprio evento) jamais passa no
 * filtro — não é necessário um parâmetro de exclusão por id (ver docs/phases/FASE-4-historical-analyzer.md §11).
 */
@Component
public class HistoricalAnalyzerImpl implements HistoricalAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(HistoricalAnalyzerImpl.class);

    private final HistoricalFeatureStoreRepository featureStoreRepository;
    private final HistoricalOutcomeRepository outcomeRepository;
    private final HistoricalFeatureNormalizer normalizer;
    private final SimilarityCalculator similarityCalculator;
    private final HistoricalProperties properties;
    private final MarketProperties marketProperties;
    private final MarketFeatureRepository marketFeatureRepository;
    private final FeatureProperties featureProperties;

    public HistoricalAnalyzerImpl(HistoricalFeatureStoreRepository featureStoreRepository,
                                  HistoricalOutcomeRepository outcomeRepository,
                                  HistoricalFeatureNormalizer normalizer,
                                  SimilarityCalculator similarityCalculator,
                                  HistoricalProperties properties,
                                  MarketProperties marketProperties,
                                  MarketFeatureRepository marketFeatureRepository,
                                  FeatureProperties featureProperties) {
        this.featureStoreRepository = featureStoreRepository;
        this.outcomeRepository = outcomeRepository;
        this.normalizer = normalizer;
        this.similarityCalculator = similarityCalculator;
        this.properties = properties;
        this.marketProperties = marketProperties;
        this.marketFeatureRepository = marketFeatureRepository;
        this.featureProperties = featureProperties;
    }

    @Override
    public HistoricalContext findSimilar(DetectedEvent event, Instant asOf) {
        String version = properties.getVersion();
        Optional<Map<String, BigDecimal>> currentNormalized = normalizeCurrentEvent(event);
        if (currentNormalized.isEmpty()) {
            log.warn("historical_context_unavailable symbol={} eventType={} reason=missing-feature-snapshot",
                    event.symbol(), event.eventType());
            return HistoricalContext.empty(version);
        }

        List<HistoricalFeatureStoreEntity> candidates = featureStoreRepository.findCandidates(
                event.symbol(), event.eventType(), asOf);

        record Scored(HistoricalFeatureStoreEntity entity, BigDecimal distance) {
        }

        List<Scored> scored = candidates.stream()
                .map(candidate -> new Scored(candidate,
                        similarityCalculator.distance(currentNormalized.get(), candidate.getNormalizedFeatures(), properties.getSimilarity().getWeights())))
                .sorted(Comparator.comparing(Scored::distance))
                .limit(properties.getSimilarity().getMaxSimilarEvents())
                .toList();

        if (scored.size() < properties.getSimilarity().getMinSampleSize()) {
            log.info("historical_context_unavailable symbol={} eventType={} sampleSize={} minSampleSize={}",
                    event.symbol(), event.eventType(), scored.size(), properties.getSimilarity().getMinSampleSize());
            return HistoricalContext.empty(version);
        }

        List<SimilarEvent> similarEvents = scored.stream()
                .map(s -> new SimilarEvent(s.entity().getEventId(), s.entity().getEventTime(),
                        similarityCalculator.similarityScore(s.distance())))
                .toList();
        List<UUID> eventIds = scored.stream().map(s -> s.entity().getEventId()).toList();

        Map<String, OutcomeStats> outcomesByHorizon = new LinkedHashMap<>();
        for (String horizon : HistoricalHorizons.LABELS) {
            aggregateHorizon(eventIds, horizon, asOf).ifPresent(stats -> outcomesByHorizon.put(horizon, stats));
        }

        log.info("historical_context_computed symbol={} eventType={} sampleSize={} horizonsAvailable={}",
                event.symbol(), event.eventType(), similarEvents.size(), outcomesByHorizon.size());
        return new HistoricalContext(similarEvents.size(), similarEvents, outcomesByHorizon, version);
    }

    private Optional<Map<String, BigDecimal>> normalizeCurrentEvent(DetectedEvent event) {
        var instrument = marketProperties.findEnabledBySymbol(event.symbol());
        if (instrument == null) {
            return Optional.empty();
        }
        Optional<FeatureSnapshot> features = marketFeatureRepository
                .findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
                        event.symbol(), instrument.interval(), event.eventTime(), featureProperties.getVersion())
                .map(entity -> entity.toSnapshot());
        return features.map(f -> normalizer.normalize(event, f));
    }

    private Optional<OutcomeStats> aggregateHorizon(List<UUID> eventIds, String horizon, Instant asOf) {
        List<HistoricalOutcomeEntity> known = outcomeRepository.findKnownAsOf(eventIds, horizon, asOf);
        if (known.isEmpty()) {
            return Optional.empty();
        }

        List<Boolean> resolved = known.stream().map(HistoricalOutcomeEntity::getTargetBeforeStop)
                .filter(v -> v != null).toList();
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        long targetWins = resolved.stream().filter(Boolean::booleanValue).count();
        BigDecimal probability = BigDecimal.valueOf(targetWins).divide(BigDecimal.valueOf(resolved.size()), MathContext.DECIMAL64);

        List<Long> timesToTargetSeconds = known.stream()
                .filter(o -> Boolean.TRUE.equals(o.getTargetBeforeStop()) && o.getTimeToTargetSeconds() != null)
                .map(HistoricalOutcomeEntity::getTimeToTargetSeconds)
                .toList();
        if (timesToTargetSeconds.isEmpty()) {
            return Optional.empty();
        }
        Duration medianTimeToTarget = Duration.ofSeconds(medianLong(timesToTargetSeconds));

        List<BigDecimal> maeValues = known.stream().map(HistoricalOutcomeEntity::getMae)
                .filter(v -> v != null).toList();
        if (maeValues.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal medianAdverseMove = medianDecimal(maeValues);

        return Optional.of(new OutcomeStats(probability, medianTimeToTarget, medianAdverseMove));
    }

    private static long medianLong(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2;
        }
        return sorted.get(middle);
    }

    private static BigDecimal medianDecimal(List<BigDecimal> values) {
        List<BigDecimal> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64);
        }
        return sorted.get(middle);
    }
}
