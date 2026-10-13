package com.btcassistant.event;

import com.btcassistant.feature.FeatureNames;
import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleIngestionService;
import com.btcassistant.market.CandleRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida o pipeline real market → feature → event (candle persistido dispara, via eventos Spring
 * reais, o cálculo de features e a detecção de eventos) e o roundtrip de {@code features_snapshot}
 * como JSONB contra PostgreSQL real (SDD Fase 2 §3/§8).
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class EventDetectorIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private CandleIngestionService candleIngestionService;

    @Autowired
    private MarketEventRepository eventRepository;

    private static Candle candleAt(int minuteOffset, String close) {
        Instant openTime = BASE_TIME.plusSeconds(60L * minuteOffset);
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal("10"), new BigDecimal("1000"), 1, "BINANCE_WS");
    }

    @Test
    void candlePersistedTriggersFeatureCalculationAndEventDetectionEndToEnd() {
        // 15 candles de queda constante (100 -> 90, -10%) seguidos pelo candle que fecha a janela de 15m
        for (int i = -15; i <= -1; i++) {
            candleRepository.save(CandleEntity.from(candleAt(i, "100")));
        }
        candleIngestionService.onCandle(candleAt(0, "90"));

        List<MarketEventEntity> events = eventRepository.findTop50BySymbolOrderByEventTimeDesc("BTCUSDT");
        Optional<MarketEventEntity> rapidDrop = events.stream()
                .filter(e -> e.getEventType().equals(EventTypes.RAPID_DROP))
                .findFirst();

        assertThat(rapidDrop).isPresent();
        assertThat(rapidDrop.get().getPrice()).isEqualByComparingTo("90");
        assertThat(rapidDrop.get().getEngineVersion()).isEqualTo("1.0");
        // roundtrip JSONB: o snapshot persistido deve vir de volta com os valores corretos
        assertThat(rapidDrop.get().getFeaturesSnapshot().get(FeatureNames.RETURN_15M))
                .isEqualByComparingTo(new BigDecimal("90").subtract(new BigDecimal("100"))
                        .divide(new BigDecimal("100"), java.math.MathContext.DECIMAL64));
    }

    @Test
    void twoDetectedEventsWithSameTypeAndTimeAreBothInsertedAsDistinctImmutableRows() {
        DetectedEvent first = new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, BASE_TIME,
                new BigDecimal("100"), java.util.Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
        DetectedEvent second = new DetectedEvent("BTCUSDT", EventTypes.RAPID_DROP, BASE_TIME,
                new BigDecimal("100"), java.util.Map.of(), new BigDecimal("0.09"), Duration.ofMinutes(15), "1.0");

        eventRepository.save(MarketEventEntity.from(first));
        eventRepository.save(MarketEventEntity.from(second));

        List<MarketEventEntity> events = eventRepository.findTop50BySymbolOrderByEventTimeDesc("BTCUSDT");
        assertThat(events).hasSize(2);
        assertThat(events).extracting(MarketEventEntity::getEventId).doesNotHaveDuplicates();
        assertThat(events).extracting(MarketEventEntity::getMagnitude)
                .containsExactlyInAnyOrder(new BigDecimal("0.03"), new BigDecimal("0.09"));
    }
}
