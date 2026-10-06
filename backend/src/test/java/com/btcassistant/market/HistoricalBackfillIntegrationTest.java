package com.btcassistant.market;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida o mecanismo de Initial Historical Backfill de ponta a ponta contra PostgreSQL real
 * (SDD Fase 0 §10.2/§10.3) — banco vazio, histórico parcial, reexecução idempotente e gap recovery.
 * A Binance real nunca é chamada: {@link StubMarketDataProvider} substitui o provider de produção.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class HistoricalBackfillIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final Instrument INSTRUMENT = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);

    @Autowired
    private HistoricalBackfillService historicalBackfillService;

    @Autowired
    private GapRecoveryService gapRecoveryService;

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private StubMarketDataProvider stubMarketDataProvider;

    private static MarketProperties.HistoricalBackfillConfig configFor(String period) {
        MarketProperties.HistoricalBackfillConfig config = new MarketProperties.HistoricalBackfillConfig();
        config.setEnabled(true);
        config.setPeriod(period);
        return config;
    }

    // Cenário 1 (doc de fechamento §10.3): banco vazio -> backfill completo automático.
    @Test
    void emptyDatabaseTriggersFullBackfillAndSecondRunDoesNotDuplicateOrRefetch() {
        MarketProperties.HistoricalBackfillConfig config = configFor("1-days");

        historicalBackfillService.ensureHistoricalData(INSTRUMENT, config);

        long firstRunCount = candleRepository.countBySymbolAndInterval("BTCUSDT", "1m");
        assertThat(firstRunCount).isBetween(1430L, 1441L);
        int callsAfterFirstRun = stubMarketDataProvider.fetchCallCount();

        // Cenário 2: histórico já completo -> reexecução não deve rebaixar nem duplicar.
        historicalBackfillService.ensureHistoricalData(INSTRUMENT, config);

        assertThat(candleRepository.countBySymbolAndInterval("BTCUSDT", "1m")).isEqualTo(firstRunCount);
        assertThat(stubMarketDataProvider.fetchCallCount()).isEqualTo(callsAfterFirstRun);
    }

    // Cenário 3: histórico parcial (buraco no meio) -> apenas o trecho ausente é buscado.
    @Test
    void partialHistoryFillsOnlyTheMissingGap() {
        MarketProperties.HistoricalBackfillConfig config = configFor("1-days");
        historicalBackfillService.ensureHistoricalData(INSTRUMENT, config);
        long fullCount = candleRepository.countBySymbolAndInterval("BTCUSDT", "1m");

        List<Instant> openTimes = candleRepository
                .findOpenTimesInRange("BTCUSDT", "1m", Instant.EPOCH, Instant.now())
                .stream().sorted(Comparator.naturalOrder()).toList();
        Instant gapStart = openTimes.get(openTimes.size() / 2);
        for (int i = 0; i < 10; i++) {
            candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", gapStart.plusSeconds(i * 60L))
                    .ifPresent(candleRepository::delete);
        }
        long afterDeletion = candleRepository.countBySymbolAndInterval("BTCUSDT", "1m");
        assertThat(afterDeletion).isEqualTo(fullCount - 10);

        historicalBackfillService.ensureHistoricalData(INSTRUMENT, config);

        assertThat(candleRepository.countBySymbolAndInterval("BTCUSDT", "1m")).isEqualTo(fullCount);
    }

    // Cenário 5: gap detectado pelo WebSocket -> recuperado via REST sem duplicar o que já existe.
    @Test
    void gapRecoveryFillsDetectedGapWithoutDuplicatingExistingCandles() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = from.plusSeconds(5 * 60);
        candleRepository.save(CandleEntity.from(stubCandleAt(from)));
        candleRepository.save(CandleEntity.from(stubCandleAt(to)));

        gapRecoveryService.recover("BTCUSDT", "1m", from, to);

        List<Instant> filled = candleRepository.findOpenTimesInRange("BTCUSDT", "1m", from, to);
        assertThat(filled).hasSize(6).doesNotHaveDuplicates();
    }

    private static Candle stubCandleAt(Instant openTime) {
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new java.math.BigDecimal("100"), new java.math.BigDecimal("110"), new java.math.BigDecimal("90"),
                new java.math.BigDecimal("105"), new java.math.BigDecimal("10"), new java.math.BigDecimal("1000"),
                50, "STUB");
    }
}
