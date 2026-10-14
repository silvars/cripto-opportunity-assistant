package com.btcassistant.opportunity.web;

import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.EvidenceQuality;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.MarketOpportunityRepository;
import com.btcassistant.opportunity.OpportunityResult;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpportunityControllerTest {

    @Mock
    private MarketOpportunityRepository repository;

    @Test
    void returnsMappedRecentOpportunities() {
        OpportunityResult result = new OpportunityResult(Direction.BUY, Classification.SETUP, new BigDecimal("70"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.5"), EvidenceQuality.LIMITED, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.MEDIUM, List.of("high_volatility"));
        MarketOpportunityEntity entity = MarketOpportunityEntity.from(UUID.randomUUID(), "BTCUSDT", result, risk);
        when(repository.findTop50BySymbolOrderByCreatedAtDesc("BTCUSDT")).thenReturn(List.of(entity));

        List<OpportunityResponse> responses = new OpportunityController(repository).recent("BTCUSDT");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).classification()).isEqualTo(Classification.SETUP);
        assertThat(responses.get(0).riskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }
}
