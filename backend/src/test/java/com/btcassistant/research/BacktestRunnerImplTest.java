package com.btcassistant.research;

import com.btcassistant.event.EventProperties;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.historical.HistoricalProperties;
import com.btcassistant.historical.ResearchOutcomeProperties;
import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import com.btcassistant.opportunity.OpportunityProperties;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DoD Fase 7 §8: reprodutibilidade (mesma config duas vezes → resultado idêntico) e no-lookahead
 * end-to-end (adicionar candles futuros além de {@code to} não muda o resultado) — mesmo teste da
 * Fase 4, agora aplicado sobre o {@link BacktestRunner} completo, via Postgres real.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class BacktestRunnerImplTest {

    private static final String SYMBOL = "BTCUSDT";
    private static final String INTERVAL = "1m";
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

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
    private BacktestRunner backtestRunner;
    @Autowired
    private EventProperties eventProperties;
    @Autowired
    private FeatureProperties featureProperties;
    @Autowired
    private OpportunityProperties opportunityProperties;
    @Autowired
    private HistoricalProperties historicalProperties;
    @Autowired
    private ResearchOutcomeProperties outcomeProperties;

    private static Candle flatCandle(Instant openTime, BigDecimal price) {
        return new Candle(SYMBOL, INTERVAL, openTime, openTime.plusSeconds(59), price, price, price, price,
                new BigDecimal("10"), new BigDecimal("500000"), 100, "BINANCE");
    }

    /** 21min de aquecimento plano + 1 candle com salto de +4% (dispara RAPID_RISE) + 130min de cauda plana. */
    private void seedBaseDataset() {
        List<CandleEntity> candles = new ArrayList<>();
        for (int minute = 0; minute <= 20; minute++) {
            candles.add(CandleEntity.from(flatCandle(T0.plusSeconds(60L * minute), new BigDecimal("50000"))));
        }
        candles.add(CandleEntity.from(flatCandle(T0.plusSeconds(60L * 21), new BigDecimal("52000"))));
        for (int minute = 22; minute <= 151; minute++) {
            candles.add(CandleEntity.from(flatCandle(T0.plusSeconds(60L * minute), new BigDecimal("52000"))));
        }
        candleRepository.saveAll(candles);
    }

    private BacktestConfig config(Instant to) {
        return new BacktestConfig(SYMBOL, T0, to, eventProperties.getEngineVersion(), featureProperties.getVersion(),
                opportunityProperties.getConfigVersion(), historicalProperties.getVersion(),
                outcomeProperties.getTargetDefinitionVersion(), outcomeProperties.getStopDefinitionVersion(), false);
    }

    @Test
    void detectsRapidRiseAndAggregatesResolvedHorizons() {
        seedBaseDataset();
        Instant to = T0.plusSeconds(60L * 151);

        BacktestResult result = backtestRunner.run(config(to));

        assertThat(result.candlesProcessed()).isEqualTo(152);
        assertThat(result.eventsDetected()).isGreaterThanOrEqualTo(1);
        assertThat(result.summaryByEventType()).containsKey(com.btcassistant.event.EventTypes.RAPID_RISE);
        EventTypeSummary rapidRise = result.summaryByEventType().get(com.btcassistant.event.EventTypes.RAPID_RISE);
        // return_15m fica acima do threshold em várias janelas consecutivas após o salto (sem dedup
        // no nível da regra — dedup/cooldown é responsabilidade do AlertService, fora do escopo do backtest)
        assertThat(rapidRise.occurrences()).isGreaterThanOrEqualTo(1);
        assertThat(rapidRise.statsByHorizon()).containsKey("5m");
        assertThat(rapidRise.statsByHorizon().get("5m").meanFutureReturn()).isEqualByComparingTo(BigDecimal.ZERO);
        // 4h/12h/24h não resolvem — a cauda de candles não chega lá
        assertThat(rapidRise.statsByHorizon()).doesNotContainKey("4h");
    }

    @Test
    void sameConfigTwiceProducesIdenticalSubstantiveResult() {
        seedBaseDataset();
        Instant to = T0.plusSeconds(60L * 151);
        BacktestConfig cfg = config(to);

        BacktestResult first = backtestRunner.run(cfg);
        BacktestResult second = backtestRunner.run(cfg);

        assertThat(second.candlesProcessed()).isEqualTo(first.candlesProcessed());
        assertThat(second.eventsDetected()).isEqualTo(first.eventsDetected());
        assertThat(second.opportunitiesGenerated()).isEqualTo(first.opportunitiesGenerated());
        assertThat(second.summaryByEventType()).isEqualTo(first.summaryByEventType());
    }

    @Test
    void addingFutureCandlesBeyondToNeverChangesTheResult() {
        seedBaseDataset();
        Instant to = T0.plusSeconds(60L * 151);
        BacktestConfig cfg = config(to);

        BacktestResult before = backtestRunner.run(cfg);

        // crash sintético bem depois do limite declarado do backtest
        List<CandleEntity> crash = new ArrayList<>();
        for (int minute = 152; minute <= 300; minute++) {
            crash.add(CandleEntity.from(flatCandle(T0.plusSeconds(60L * minute), new BigDecimal("10000"))));
        }
        candleRepository.saveAll(crash);

        BacktestResult after = backtestRunner.run(cfg);

        assertThat(after.candlesProcessed()).isEqualTo(before.candlesProcessed());
        assertThat(after.eventsDetected()).isEqualTo(before.eventsDetected());
        assertThat(after.opportunitiesGenerated()).isEqualTo(before.opportunitiesGenerated());
        assertThat(after.summaryByEventType()).isEqualTo(before.summaryByEventType());
    }

    @Test
    void rejectsMismatchedVersionsInsteadOfSilentlySwitchingConfig() {
        seedBaseDataset();
        Instant to = T0.plusSeconds(60L * 151);
        BacktestConfig mismatched = new BacktestConfig(SYMBOL, T0, to, "9.9", featureProperties.getVersion(),
                opportunityProperties.getConfigVersion(), historicalProperties.getVersion(),
                outcomeProperties.getTargetDefinitionVersion(), outcomeProperties.getStopDefinitionVersion(), false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> backtestRunner.run(mismatched))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("engineVersion");
    }
}
