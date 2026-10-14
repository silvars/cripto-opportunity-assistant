package com.btcassistant.decision;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
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
import com.btcassistant.web.NotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Decisão é um fato separado do alerta — nunca o muta (SDD §57). */
@ExtendWith(MockitoExtension.class)
class UserDecisionServiceTest {

    @Mock
    private UserDecisionRepository repository;
    @Mock
    private AlertRepository alertRepository;

    private UserDecisionService service;

    @BeforeEach
    void setUp() {
        service = new UserDecisionService(repository, alertRepository);
        org.mockito.Mockito.lenient().when(repository.save(any(UserDecisionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AlertEntity alert() {
        OpportunityResult result = new OpportunityResult(Direction.BUY, Classification.SETUP, new BigDecimal("80"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.8"), EvidenceQuality.FULL, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        MarketOpportunityEntity opportunity = MarketOpportunityEntity.from(UUID.randomUUID(), "BTCUSDT", result, risk);
        DetectedEvent event = new DetectedEvent("BTCUSDT", EventTypes.RAPID_RISE, Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("50000"), Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
        return AlertEntity.from(opportunity, event, Instant.parse("2026-01-01T00:15:00Z"));
    }

    @Test
    void registersDecisionInheritingExecutionModeFromAlert() {
        AlertEntity alert = alert();
        when(alertRepository.findById(alert.getAlertId())).thenReturn(Optional.of(alert));

        UserDecisionEntity result = service.register(alert.getAlertId(), Decision.BUY);

        assertThat(result.getAlertId()).isEqualTo(alert.getAlertId());
        assertThat(result.getDecision()).isEqualTo(Decision.BUY);
        assertThat(result.getExecutionMode()).isEqualTo(ExecutionMode.LIVE);
    }

    @Test
    void throwsNotFoundWhenAlertDoesNotExist() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(alertId, Decision.IGNORE))
                .isInstanceOf(NotFoundException.class);
    }
}
