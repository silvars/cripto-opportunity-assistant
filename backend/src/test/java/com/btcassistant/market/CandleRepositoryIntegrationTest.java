package com.btcassistant.market;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Valida migrations, constraints e queries de cobertura contra PostgreSQL real (SDD Fase 0 §10.2). */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CandleRepositoryIntegrationTest {

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

    private static CandleEntity entity(Instant openTime) {
        return CandleEntity.from(new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "BINANCE_REST"));
    }

    @Test
    void migrationCreatesCandlesTableWithUniqueConstraint() {
        Instant openTime = Instant.parse("2026-01-01T00:00:00Z");
        candleRepository.saveAndFlush(entity(openTime));

        assertThatThrownBy(() -> candleRepository.saveAndFlush(entity(openTime)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsAndReadsBackCandleFields() {
        Instant openTime = Instant.parse("2026-02-01T10:15:00Z");
        candleRepository.save(entity(openTime));

        CandleEntity found = candleRepository.findBySymbolAndIntervalAndOpenTime("BTCUSDT", "1m", openTime)
                .orElseThrow();

        assertThat(found.getClose()).isEqualByComparingTo(new BigDecimal("105"));
        assertThat(found.getSource()).isEqualTo("BINANCE_REST");
    }

    @Test
    void findOpenTimesInRangeReturnsOrderedTimestamps() {
        Instant base = Instant.parse("2026-03-01T00:00:00Z");
        candleRepository.save(entity(base.plusSeconds(120)));
        candleRepository.save(entity(base));
        candleRepository.save(entity(base.plusSeconds(60)));

        List<Instant> openTimes = candleRepository.findOpenTimesInRange("BTCUSDT", "1m", base, base.plusSeconds(600));

        assertThat(openTimes).containsExactly(base, base.plusSeconds(60), base.plusSeconds(120));
    }

    @Test
    void countAndBoundaryQueriesReflectPersistedData() {
        Instant base = Instant.parse("2026-04-01T00:00:00Z");
        candleRepository.save(entity(base));
        candleRepository.save(entity(base.plusSeconds(60)));
        candleRepository.save(entity(base.plusSeconds(120)));

        assertThat(candleRepository.countBySymbolAndInterval("BTCUSDT", "1m")).isEqualTo(3L);
        assertThat(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeAsc("BTCUSDT", "1m"))
                .map(CandleEntity::getOpenTime).contains(base);
        assertThat(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc("BTCUSDT", "1m"))
                .map(CandleEntity::getOpenTime).contains(base.plusSeconds(120));
    }
}
