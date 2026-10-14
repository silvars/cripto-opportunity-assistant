package com.btcassistant.pipeline;

import com.btcassistant.alert.AlertRepository;
import com.btcassistant.event.MarketEventRepository;
import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleIngestionService;
import com.btcassistant.market.CandleRepository;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.MarketOpportunityRepository;
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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida o pipeline completo e real market → feature → event → opportunity → risk → alert (SDD
 * Fase 3 §8 — "pipeline evento → oportunidade → risco → alerta persistido") e a regra central do
 * DoD: um movimento prolongado no mesmo símbolo+direção não deve gerar alertas duplicados.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class OpportunityPipelineIntegrationTest {

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

    @Autowired
    private MarketOpportunityRepository opportunityRepository;

    @Autowired
    private AlertRepository alertRepository;

    private static Candle candleAt(int minuteOffset, String close) {
        Instant openTime = BASE_TIME.plusSeconds(60L * minuteOffset);
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal("10"), new BigDecimal("1000"), 1, "BINANCE_WS");
    }

    @Test
    void prolongedDropDoesNotCreateDuplicateAlertsForSameSymbolAndDirection() {
        for (int i = -15; i <= -1; i++) {
            candleRepository.save(CandleEntity.from(candleAt(i, "100")));
        }

        candleIngestionService.onCandle(candleAt(0, "90"));
        candleIngestionService.onCandle(candleAt(1, "85"));

        assertThat(eventRepository.findTop50BySymbolOrderByEventTimeDesc("BTCUSDT")).hasSizeGreaterThanOrEqualTo(4);

        var opportunities = opportunityRepository.findTop50BySymbolOrderByCreatedAtDesc("BTCUSDT");
        assertThat(opportunities).hasSizeGreaterThanOrEqualTo(4);
        assertThat(opportunities.get(0).getScoreComponents()).containsKey("eventStrength");
        assertThat(opportunities.get(0).getRiskReasons()).isNotEmpty();

        var alerts = alertRepository.findTop50BySymbolOrderByCreatedAtDesc("BTCUSDT");
        assertThat(alerts).hasSize(2);
        assertThat(alerts).extracting(a -> a.getDirection()).containsExactlyInAnyOrder(Direction.SELL, Direction.NEUTRAL);
        alerts.forEach(alert -> assertThat(alert.getFeatureSnapshot()).isNotEmpty());
    }
}
