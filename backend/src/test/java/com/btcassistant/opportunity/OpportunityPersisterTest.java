package com.btcassistant.opportunity;

import com.btcassistant.risk.RiskAssessment;
import com.btcassistant.risk.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpportunityPersisterTest {

    @Mock
    private MarketOpportunityRepository repository;

    @Test
    void savesEntityBuiltFromResultAndRiskAssessment() {
        OpportunityPersister persister = new OpportunityPersister(repository);
        OpportunityResult result = new OpportunityResult(Direction.BUY, Classification.WATCH, new BigDecimal("55"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.5"), EvidenceQuality.LIMITED, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        UUID eventId = UUID.randomUUID();
        when(repository.save(any(MarketOpportunityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MarketOpportunityEntity saved = persister.persist(eventId, "BTCUSDT", result, risk);

        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(saved.getClassification()).isEqualTo(Classification.WATCH);
        assertThat(saved.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        verify(repository).save(any(MarketOpportunityEntity.class));
    }
}
