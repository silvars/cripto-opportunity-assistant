package com.btcassistant.operation;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.decision.Decision;
import com.btcassistant.decision.UserDecisionEntity;
import com.btcassistant.decision.UserDecisionService;
import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.event.MarketEventEntity;
import com.btcassistant.event.MarketEventRepository;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.EvidenceQuality;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.MarketOpportunityRepository;
import com.btcassistant.opportunity.OpportunityResult;
import com.btcassistant.risk.RiskAssessment;
import com.btcassistant.risk.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxo completo decisão → operação (entrada) → operação (saída) → screenshot → P&L (DoD Fase 6
 * §8), com Postgres real via Testcontainers — valida FKs reais entre alerts/user_decisions/
 * trades/operations/operation_screenshots.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class DecisionOperationFlowIntegrationTest {

    private static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @TempDir
    static Path screenshotsDir;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("operations.screenshots.storage-path", () -> screenshotsDir.toString());
    }

    @Autowired
    private MarketOpportunityRepository opportunityRepository;
    @Autowired
    private MarketEventRepository eventRepository;
    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private UserDecisionService decisionService;
    @Autowired
    private OperationService operationService;
    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private OperationScreenshotRepository screenshotRepository;
    @Autowired
    private PnlCalculator pnlCalculator;
    @Autowired
    private OperationProperties operationProperties;

    private AlertEntity persistAlert() {
        DetectedEvent event = new DetectedEvent("BTCUSDT", EventTypes.RAPID_RISE, EVENT_TIME, new BigDecimal("50000"),
                Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
        MarketEventEntity eventEntity = MarketEventEntity.from(event);
        eventRepository.save(eventEntity);

        OpportunityResult result = new OpportunityResult(Direction.BUY, Classification.SETUP, new BigDecimal("80"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.8"), EvidenceQuality.FULL, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        MarketOpportunityEntity opportunity = MarketOpportunityEntity.from(eventEntity.getEventId(), "BTCUSDT", result, risk);
        opportunityRepository.save(opportunity);
        AlertEntity alert = AlertEntity.from(opportunity, event, EVENT_TIME.plus(Duration.ofMinutes(15)));
        alertRepository.save(alert);
        return alert;
    }

    @Test
    void fullFlowFromDecisionToProfitAndLoss() throws Exception {
        AlertEntity alert = persistAlert();

        UserDecisionEntity decision = decisionService.register(alert.getAlertId(), Decision.BUY);

        OperationEntity entry = operationService.registerOperation(decision.getDecisionId(), Side.BUY,
                new BigDecimal("1"), new BigDecimal("50000"), null, null, EVENT_TIME);

        TradeEntity afterEntry = tradeRepository.findByDecisionId(decision.getDecisionId()).orElseThrow();
        assertThat(afterEntry.getStatus()).isEqualTo(TradeStatus.OPEN);
        assertThat(afterEntry.getEntryOperationId()).isEqualTo(entry.getOperationId());

        MultipartFile screenshotFile = new MockMultipartFile("file", "entry.png", "image/png", new byte[]{1, 2, 3, 4});
        OperationScreenshotEntity screenshot = operationService.attachScreenshot(entry.getOperationId(), screenshotFile);
        assertThat(screenshotRepository.findById(screenshot.getScreenshotId())).isPresent();
        assertThat(Files.exists(Path.of(screenshot.getFilePath()))).isTrue();

        OperationEntity exit = operationService.registerOperation(decision.getDecisionId(), Side.SELL,
                new BigDecimal("1"), new BigDecimal("51000"), null, null, EVENT_TIME.plus(Duration.ofHours(3)));

        TradeEntity closedTrade = tradeRepository.findByDecisionId(decision.getDecisionId()).orElseThrow();
        assertThat(closedTrade.getStatus()).isEqualTo(TradeStatus.CLOSED);
        assertThat(closedTrade.getExitOperationId()).isEqualTo(exit.getOperationId());

        PnlResult pnl = pnlCalculator.calculate(closedTrade, entry, exit);
        assertThat(pnl.grossPnl()).isEqualByComparingTo("1000");
        BigDecimal expectedFees = new BigDecimal("50000").multiply(operationProperties.getDefaultFeeRate())
                .add(new BigDecimal("51000").multiply(operationProperties.getDefaultFeeRate()));
        assertThat(pnl.fees()).isEqualByComparingTo(expectedFees);
        assertThat(pnl.netPnl()).isLessThan(pnl.grossPnl());
        assertThat(pnl.holdingTime()).isEqualTo(Duration.ofHours(3));
    }
}
