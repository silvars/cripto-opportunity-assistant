package com.btcassistant.research;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.decision.Decision;
import com.btcassistant.decision.UserDecisionEntity;
import com.btcassistant.decision.UserDecisionRepository;
import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.EvidenceQuality;
import com.btcassistant.opportunity.ExecutionMode;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.OpportunityResult;
import com.btcassistant.risk.RiskAssessment;
import com.btcassistant.risk.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** "Alert ignorado + mercado se moveu a favor" (SDD §56). */
@ExtendWith(MockitoExtension.class)
class MissedOpportunityServiceTest {

    private static final Instant ALERT_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private UserDecisionRepository decisionRepository;
    @Mock
    private AlertEvaluationRepository evaluationRepository;

    private ResearchProperties properties;
    private MissedOpportunityService service;

    @BeforeEach
    void setUp() {
        properties = new ResearchProperties();
        properties.getEvaluation().setCanonicalHorizon("24h");
        service = new MissedOpportunityService(alertRepository, decisionRepository, evaluationRepository, properties);
    }

    private AlertEntity alert(Direction direction) {
        OpportunityResult result = new OpportunityResult(direction, Classification.SETUP, new BigDecimal("80"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.8"), EvidenceQuality.FULL, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        MarketOpportunityEntity opportunity = MarketOpportunityEntity.from(UUID.randomUUID(), "BTCUSDT", result, risk);
        DetectedEvent event = new DetectedEvent("BTCUSDT", EventTypes.RAPID_RISE, ALERT_TIME, new BigDecimal("50000"),
                Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
        return AlertEntity.from(opportunity, event, ALERT_TIME.plus(Duration.ofMinutes(15)));
    }

    private AlertEvaluationEntity evaluationWithCanonicalReturn(UUID alertId, BigDecimal value) {
        AlertEvaluationEntity evaluation = AlertEvaluationEntity.create(alertId, ExecutionMode.LIVE);
        evaluation.setHorizonReturn("24h", value);
        return evaluation;
    }

    @Test
    void includesIgnoredAlertThatMovedFavorably() {
        AlertEntity alert = alert(Direction.BUY);
        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(decisionRepository.findByAlertIdOrderByCreatedAtDesc(alert.getAlertId()))
                .thenReturn(List.of(UserDecisionEntity.create(alert.getAlertId(), Decision.IGNORE, ExecutionMode.LIVE)));
        when(evaluationRepository.findById(alert.getAlertId()))
                .thenReturn(Optional.of(evaluationWithCanonicalReturn(alert.getAlertId(), new BigDecimal("0.05"))));

        List<MissedOpportunity> result = service.find();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).favorableMove()).isEqualByComparingTo("0.05");
    }

    @Test
    void treatsAlertWithoutAnyDecisionAsIgnored() {
        AlertEntity alert = alert(Direction.BUY);
        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(decisionRepository.findByAlertIdOrderByCreatedAtDesc(alert.getAlertId())).thenReturn(List.of());
        when(evaluationRepository.findById(alert.getAlertId()))
                .thenReturn(Optional.of(evaluationWithCanonicalReturn(alert.getAlertId(), new BigDecimal("0.03"))));

        assertThat(service.find()).hasSize(1);
    }

    @Test
    void excludesAlertsTheUserActedOn() {
        AlertEntity alert = alert(Direction.BUY);
        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(decisionRepository.findByAlertIdOrderByCreatedAtDesc(alert.getAlertId()))
                .thenReturn(List.of(UserDecisionEntity.create(alert.getAlertId(), Decision.BUY, ExecutionMode.LIVE)));

        assertThat(service.find()).isEmpty();
    }

    @Test
    void excludesAlertsNotYetEvaluated() {
        AlertEntity alert = alert(Direction.BUY);
        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(decisionRepository.findByAlertIdOrderByCreatedAtDesc(alert.getAlertId())).thenReturn(List.of());
        when(evaluationRepository.findById(alert.getAlertId())).thenReturn(Optional.empty());

        assertThat(service.find()).isEmpty();
    }

    @Test
    void flipsSignForSellDirectionBeforeComparing() {
        AlertEntity alert = alert(Direction.SELL);
        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(decisionRepository.findByAlertIdOrderByCreatedAtDesc(alert.getAlertId())).thenReturn(List.of());
        // mercado caiu 5% depois do alerta de SELL -> favorável pra quem teria vendido
        when(evaluationRepository.findById(alert.getAlertId()))
                .thenReturn(Optional.of(evaluationWithCanonicalReturn(alert.getAlertId(), new BigDecimal("-0.05"))));

        List<MissedOpportunity> result = service.find();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).favorableMove()).isEqualByComparingTo("0.05");
    }

    @Test
    void excludesUnfavorableMoves() {
        AlertEntity alert = alert(Direction.BUY);
        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(decisionRepository.findByAlertIdOrderByCreatedAtDesc(alert.getAlertId())).thenReturn(List.of());
        when(evaluationRepository.findById(alert.getAlertId()))
                .thenReturn(Optional.of(evaluationWithCanonicalReturn(alert.getAlertId(), new BigDecimal("-0.02"))));

        assertThat(service.find()).isEmpty();
    }
}
