package com.btcassistant.feature;

import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import com.btcassistant.market.Instrument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida migration/constraints do {@code market_features} e, mais importante, a regra de
 * no-lookahead do Feature Engine contra PostgreSQL real (SDD Fase 1 §8 — "calcular em T, adicionar
 * candles futuros, recalcular em T, resultado idêntico").
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class FeatureEngineIntegrationTest {

    private static final Instrument INSTRUMENT = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);
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
    private FeatureCalculator featureCalculator;

    @Autowired
    private FeaturePersister featurePersister;

    @Autowired
    private MarketFeatureRepository featureRepository;

    private static void persistCandle(CandleRepository repository, int minuteOffset, String close) {
        Instant openTime = BASE_TIME.plusSeconds(60L * minuteOffset);
        Candle candle = new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal("10"), new BigDecimal("1000"), 1, "BINANCE_WS");
        repository.save(CandleEntity.from(candle));
    }

    @Test
    void recalculatingAtSameReferenceTimeAfterFutureCandlesArrivedYieldsIdenticalSnapshot() {
        for (int i = 0; i <= 5; i++) {
            persistCandle(candleRepository, i, String.valueOf(100 + i));
        }
        Instant referenceTime = BASE_TIME.plusSeconds(60L * 5);

        FeatureSnapshot before = featureCalculator.calculate(INSTRUMENT, referenceTime);

        // candles "do futuro" relativos a referenceTime chegam depois
        for (int i = 6; i <= 10; i++) {
            persistCandle(candleRepository, i, String.valueOf(200 + i));
        }

        FeatureSnapshot after = featureCalculator.calculate(INSTRUMENT, referenceTime);

        assertThat(before.values()).isEqualTo(after.values());
        assertThat(after.values().get(FeatureNames.RETURN_1M)).isEqualByComparingTo(
                new BigDecimal("105").subtract(new BigDecimal("104"))
                        .divide(new BigDecimal("104"), java.math.MathContext.DECIMAL64));
    }

    @Test
    void featurePersisterUpsertNeverDuplicatesRowForSameKey() {
        persistCandle(candleRepository, 0, "100");
        Instant referenceTime = BASE_TIME;
        FeatureSnapshot snapshot = featureCalculator.calculate(INSTRUMENT, referenceTime);

        boolean firstPersist = featurePersister.persist(snapshot);
        boolean secondPersist = featurePersister.persist(snapshot);

        assertThat(firstPersist).isTrue();
        assertThat(secondPersist).isFalse();
        assertThat(featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
                "BTCUSDT", "1m", referenceTime, snapshot.featureVersion())).isPresent();
    }

    @Test
    void migrationCreatesMarketFeaturesTableWithUniqueConstraint() {
        persistCandle(candleRepository, 0, "100");
        FeatureSnapshot snapshot = featureCalculator.calculate(INSTRUMENT, BASE_TIME);

        featureRepository.saveAndFlush(MarketFeatureEntity.from(snapshot));

        assertThatThrownBy(() -> featureRepository.saveAndFlush(MarketFeatureEntity.from(snapshot)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
