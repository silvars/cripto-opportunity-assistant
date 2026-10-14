package com.btcassistant.alert.web;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.EvidenceQuality;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.OpportunityResult;
import com.btcassistant.risk.RiskAssessment;
import com.btcassistant.risk.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertRepository repository;

    @Test
    void returnsMappedRecentAlerts() {
        OpportunityResult result = new OpportunityResult(Direction.BUY, Classification.SETUP, new BigDecimal("70"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.5"), EvidenceQuality.LIMITED, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        MarketOpportunityEntity opportunity = MarketOpportunityEntity.from(UUID.randomUUID(), "BTCUSDT", result, risk);
        DetectedEvent event = new DetectedEvent("BTCUSDT", EventTypes.RAPID_RISE, Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("100"), Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
        AlertEntity alert = AlertEntity.from(opportunity, event, Instant.now().plus(Duration.ofMinutes(15)));
        when(repository.findTop50BySymbolOrderByCreatedAtDesc("BTCUSDT")).thenReturn(List.of(alert));

        List<AlertResponse> responses = new AlertController(repository).recent("BTCUSDT");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).symbol()).isEqualTo("BTCUSDT");
        assertThat(responses.get(0).direction()).isEqualTo(Direction.BUY);
    }
}
