package com.btcassistant.agent;

import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.risk.RiskAssessment;
import com.btcassistant.risk.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Insert-only: reanálise cria analysis_version incremental, nunca sobrescreve (SDD §3). */
@ExtendWith(MockitoExtension.class)
class AgentAnalysisPersisterTest {

    @Mock
    private AgentAnalysisRepository repository;

    private AgentAnalysisPersister persister;

    @BeforeEach
    void setUp() {
        persister = new AgentAnalysisPersister(repository);
        when(repository.save(any(AgentAnalysisEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AgentAnalysis completedAnalysis() {
        return new AgentAnalysis(Classification.SETUP, Direction.BUY, new BigDecimal("80"), Confidence.HIGH,
                "Setup que merece avaliação.", List.of("evidência"), null,
                new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals")),
                List.of("invalidação"), "1.0", "1.0", AnalysisStatus.COMPLETED);
    }

    @Test
    void usesVersionOneForFirstAnalysisOfAnAlert() {
        UUID alertId = UUID.randomUUID();
        when(repository.findTopByAlertIdOrderByAnalysisVersionDesc(alertId)).thenReturn(Optional.empty());

        AgentAnalysisEntity entity = persister.persist(alertId, completedAnalysis());

        assertThat(entity.getAnalysisVersion()).isEqualTo(1);
        assertThat(entity.getAlertId()).isEqualTo(alertId);
    }

    @Test
    void incrementsVersionWhenAPreviousAnalysisExists() {
        UUID alertId = UUID.randomUUID();
        AgentAnalysisEntity previous = AgentAnalysisEntity.create(alertId, 3, completedAnalysis());
        when(repository.findTopByAlertIdOrderByAnalysisVersionDesc(alertId)).thenReturn(Optional.of(previous));

        AgentAnalysisEntity entity = persister.persist(alertId, completedAnalysis());

        assertThat(entity.getAnalysisVersion()).isEqualTo(4);
    }
}
