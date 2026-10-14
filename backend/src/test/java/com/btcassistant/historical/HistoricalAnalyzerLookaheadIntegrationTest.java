package com.btcassistant.historical;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.event.MarketEventEntity;
import com.btcassistant.event.MarketEventRepository;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.feature.MarketFeatureEntity;
import com.btcassistant.feature.MarketFeatureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SDD Fase 4 §8 — "a fase mais sensível do sistema": teste de lookahead é obrigatório e
 * bloqueante para merge. Também cobre o teste de fronteira explícito do DoD (candidato com
 * {@code eventTime == asOf} rejeitado; outcome com {@code evaluationEnd == asOf} aceito;
 * {@code evaluationEnd == asOf + 1ms} rejeitado).
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class HistoricalAnalyzerLookaheadIntegrationTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MarketEventRepository eventRepository;

    @Autowired
    private HistoricalFeatureStoreRepository featureStoreRepository;

    @Autowired
    private HistoricalOutcomeRepository outcomeRepository;

    @Autowired
    private MarketFeatureRepository marketFeatureRepository;

    @Autowired
    private HistoricalAnalyzer historicalAnalyzer;

    private UUID persistCandidateEvent(Instant eventTime, BigDecimal volumeRatio) {
        DetectedEvent detected = new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, eventTime, new BigDecimal("100"),
                Map.of(), new BigDecimal("0.05"), Duration.ofMinutes(15), "1.0");
        MarketEventEntity saved = eventRepository.save(MarketEventEntity.from(detected));
        featureStoreRepository.save(HistoricalFeatureStoreEntity.create("BTCUSDT", saved.getEventId(), EventTypes.RAPID_DROP,
                eventTime, Map.of(
                        HistoricalFeatureNames.VOLUME_RATIO, volumeRatio,
                        HistoricalFeatureNames.MAGNITUDE, new BigDecimal("0.05"),
                        HistoricalFeatureNames.DURATION, new BigDecimal("15")),
                "1.0"));
        return saved.getEventId();
    }

    @Test
    void addingFutureEventsAfterAsOfDoesNotChangeResultComputedAtT() {
        Instant asOf = BASE.plusSeconds(3600L * 48);
        for (int i = 0; i < 30; i++) {
            persistCandidateEvent(asOf.minus(Duration.ofHours(i + 1)), new BigDecimal("1.1"));
        }
        marketFeatureRepository.save(MarketFeatureEntity.from(
                new FeatureSnapshot("BTCUSDT", "1m", asOf, "1.0", Map.of(FeatureNames.VOLUME_RATIO, new BigDecimal("1.1")))));
        DetectedEvent currentEvent = new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, asOf, new BigDecimal("100"),
                Map.of(), new BigDecimal("0.05"), Duration.ofMinutes(15), "1.0");

        HistoricalContext before = historicalAnalyzer.findSimilar(currentEvent, asOf);

        // dados "do futuro" relativos a asOf chegam depois
        for (int i = 0; i < 10; i++) {
            persistCandidateEvent(asOf.plus(Duration.ofHours(i + 1)), new BigDecimal("1.1"));
        }

        HistoricalContext after = historicalAnalyzer.findSimilar(currentEvent, asOf);

        assertThat(after).isEqualTo(before);
        assertThat(after.sampleSize()).isEqualTo(30);
    }

    @Test
    void candidateWithEventTimeEqualToAsOfIsStrictlyRejected() {
        Instant asOf = BASE.plusSeconds(999_999);
        UUID atBoundary = persistCandidateEvent(asOf, new BigDecimal("1.0"));
        persistCandidateEvent(asOf.minusSeconds(1), new BigDecimal("1.0"));

        List<HistoricalFeatureStoreEntity> candidates = featureStoreRepository.findCandidates("BTCUSDT", EventTypes.RAPID_DROP, asOf);

        assertThat(candidates).extracting(HistoricalFeatureStoreEntity::getEventId).doesNotContain(atBoundary);
    }

    @Test
    void outcomeEvaluationEndExactlyAtAsOfIsAcceptedButOneMillisecondLaterIsRejected() {
        Instant asOf = BASE.plusSeconds(3600);
        UUID knownEventId = persistCandidateEvent(BASE, new BigDecimal("1.0"));
        UUID unknownEventId = persistCandidateEvent(BASE.plusMillis(1), new BigDecimal("1.0"));

        outcomeRepository.save(HistoricalOutcomeEntity.from(new HistoricalOutcomeResult(knownEventId, "1h",
                new BigDecimal("0.01"), new BigDecimal("0.02"), new BigDecimal("-0.01"),
                Duration.ofMinutes(10), null, true, asOf, "1.0", "1.0")));
        outcomeRepository.save(HistoricalOutcomeEntity.from(new HistoricalOutcomeResult(unknownEventId, "1h",
                new BigDecimal("0.01"), new BigDecimal("0.02"), new BigDecimal("-0.01"),
                Duration.ofMinutes(10), null, true, asOf.plusMillis(1), "1.0", "1.0")));

        List<HistoricalOutcomeEntity> known = outcomeRepository.findKnownAsOf(
                List.of(knownEventId, unknownEventId), "1h", asOf);

        assertThat(known).extracting(HistoricalOutcomeEntity::getEventId).contains(knownEventId).doesNotContain(unknownEventId);
    }
}
