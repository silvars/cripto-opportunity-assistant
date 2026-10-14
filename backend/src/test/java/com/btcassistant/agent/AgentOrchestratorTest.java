package com.btcassistant.agent;

import com.btcassistant.alert.AlertCreatedEvent;
import com.btcassistant.alert.AlertEntity;
import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.historical.HistoricalContext;
import com.btcassistant.historical.HistoricalProperties;
import com.btcassistant.historical.OutcomeStats;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.EvidenceQuality;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o DoD da Fase 5: só dispara acima do trigger-threshold, nunca propaga falha do provider
 * (graceful degradation — SDD §34/§35), respeita max-retries, e sempre ecoa os campos
 * determinísticos (classification/direction/score/risk) em vez de deixar o LLM decidi-los.
 */
@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    private static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID ALERT_ID = UUID.randomUUID();

    @Mock
    private AgentContextBuilder contextBuilder;
    @Mock
    private AgentProvider provider;
    @Mock
    private AgentAnalysisPersister persister;

    private final AgentProperties properties = new AgentProperties();
    private final HistoricalProperties historicalProperties = new HistoricalProperties();
    private AgentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties.setMaxRetries(1);
        properties.setTriggerThreshold(new BigDecimal("50"));
        orchestrator = new AgentOrchestrator(properties, historicalProperties, contextBuilder, provider, persister);
    }

    private AlertEntity alertWithScore(BigDecimal score) {
        AlertEntity alert = org.mockito.Mockito.mock(AlertEntity.class);
        org.mockito.Mockito.lenient().when(alert.getScore()).thenReturn(score);
        org.mockito.Mockito.lenient().when(alert.getAlertId()).thenReturn(ALERT_ID);
        org.mockito.Mockito.lenient().when(alert.getSymbol()).thenReturn("BTCUSDT");
        return alert;
    }

    private static DetectedEvent detectedEvent() {
        return new DetectedEvent("BTCUSDT", EventTypes.RAPID_RISE, EVENT_TIME, new BigDecimal("50000"),
                Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
    }

    private AgentContext agentContext(Optional<HistoricalContext> historicalContext) {
        OpportunityResult result = new OpportunityResult(Direction.BUY, Classification.SETUP, new BigDecimal("80"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.8"), EvidenceQuality.FULL, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        MarketOpportunityEntity opportunity = MarketOpportunityEntity.from(UUID.randomUUID(), "BTCUSDT", result, risk);
        InstrumentContext instrument = new InstrumentContext("BTCUSDT", "BTC", "USDT", "BINANCE", "1m");
        return new AgentContext(instrument, Map.of(), Map.of(), detectedEvent(), opportunity, risk, historicalContext, List.of());
    }

    @Test
    void doesNothingWhenAgentIsDisabled() {
        properties.setEnabled(false);
        AlertCreatedEvent event = new AlertCreatedEvent(alertWithScore(new BigDecimal("90")), detectedEvent(), Optional.empty());

        orchestrator.onAlertCreated(event);

        verify(contextBuilder, never()).build(any());
        verify(persister, never()).persist(any(), any());
    }

    @Test
    void skipsWhenScoreIsBelowTriggerThreshold() {
        AlertCreatedEvent event = new AlertCreatedEvent(alertWithScore(new BigDecimal("40")), detectedEvent(), Optional.empty());

        orchestrator.onAlertCreated(event);

        verify(contextBuilder, never()).build(any());
        verify(persister, never()).persist(any(), any());
    }

    @Test
    void skipsWhenContextCannotBeAssembled() throws Exception {
        AlertCreatedEvent event = new AlertCreatedEvent(alertWithScore(new BigDecimal("90")), detectedEvent(), Optional.empty());
        when(contextBuilder.build(event)).thenReturn(Optional.empty());

        orchestrator.onAlertCreated(event);

        verify(provider, never()).analyze(any());
        verify(persister, never()).persist(any(), any());
    }

    @Test
    void persistsCompletedAnalysisEchoingDeterministicFields() throws Exception {
        AlertCreatedEvent event = new AlertCreatedEvent(alertWithScore(new BigDecimal("90")), detectedEvent(), Optional.empty());
        AgentContext context = agentContext(Optional.empty());
        when(contextBuilder.build(event)).thenReturn(Optional.of(context));
        AgentNarrative narrative = new AgentNarrative("Setup que merece avaliação.", List.of("evidência"), Confidence.HIGH, List.of("invalidação"));
        when(provider.analyze(context)).thenReturn(narrative);

        orchestrator.onAlertCreated(event);

        org.mockito.ArgumentCaptor<AgentAnalysis> captor = org.mockito.ArgumentCaptor.forClass(AgentAnalysis.class);
        verify(persister).persist(eq(ALERT_ID), captor.capture());
        AgentAnalysis analysis = captor.getValue();
        assertThat(analysis.status()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(analysis.classification()).isEqualTo(context.opportunity().getClassification());
        assertThat(analysis.direction()).isEqualTo(context.opportunity().getDirection());
        assertThat(analysis.score()).isEqualByComparingTo(context.opportunity().getScore());
        assertThat(analysis.risk()).isEqualTo(context.risk());
        assertThat(analysis.summary()).isEqualTo("Setup que merece avaliação.");
        assertThat(analysis.confidence()).isEqualTo(Confidence.HIGH);
        verify(provider, times(1)).analyze(context);
    }

    @Test
    void retriesUpToConfiguredLimitBeforePersistingFailedStatus() throws Exception {
        properties.setMaxRetries(2);
        AlertCreatedEvent event = new AlertCreatedEvent(alertWithScore(new BigDecimal("90")), detectedEvent(), Optional.empty());
        AgentContext context = agentContext(Optional.empty());
        when(contextBuilder.build(event)).thenReturn(Optional.of(context));
        when(provider.analyze(context)).thenThrow(new AgentProviderException("falha genérica", false, null));

        orchestrator.onAlertCreated(event);

        verify(provider, times(3)).analyze(context);
        org.mockito.ArgumentCaptor<AgentAnalysis> captor = org.mockito.ArgumentCaptor.forClass(AgentAnalysis.class);
        verify(persister).persist(eq(ALERT_ID), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(AnalysisStatus.FAILED);
    }

    @Test
    void persistsTimeoutStatusWhenProviderTimesOut() throws Exception {
        AlertCreatedEvent event = new AlertCreatedEvent(alertWithScore(new BigDecimal("90")), detectedEvent(), Optional.empty());
        AgentContext context = agentContext(Optional.empty());
        when(contextBuilder.build(event)).thenReturn(Optional.of(context));
        when(provider.analyze(context)).thenThrow(new AgentProviderException("timeout", true, null));

        orchestrator.onAlertCreated(event);

        org.mockito.ArgumentCaptor<AgentAnalysis> captor = org.mockito.ArgumentCaptor.forClass(AgentAnalysis.class);
        verify(persister).persist(eq(ALERT_ID), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(AnalysisStatus.TIMEOUT);
    }

    @Test
    void neverPropagatesProviderExceptionToCaller() throws Exception {
        AlertCreatedEvent event = new AlertCreatedEvent(alertWithScore(new BigDecimal("90")), detectedEvent(), Optional.empty());
        AgentContext context = agentContext(Optional.empty());
        when(contextBuilder.build(event)).thenReturn(Optional.of(context));
        when(provider.analyze(context)).thenThrow(new AgentProviderException("boom", false, null));

        org.assertj.core.api.Assertions.assertThatCode(() -> orchestrator.onAlertCreated(event)).doesNotThrowAnyException();
    }

    @Test
    void summarizesHistoricalContextUsingCanonicalHorizon() throws Exception {
        historicalProperties.setCanonicalHorizon("1h");
        OutcomeStats canonical = new OutcomeStats(new BigDecimal("0.65"), Duration.ofMinutes(30), new BigDecimal("0.01"));
        HistoricalContext historical = new HistoricalContext(40, List.of(), Map.of("1h", canonical), "1.0");
        AlertCreatedEvent event = new AlertCreatedEvent(alertWithScore(new BigDecimal("90")), detectedEvent(), Optional.of(historical));
        AgentContext context = agentContext(Optional.of(historical));
        when(contextBuilder.build(event)).thenReturn(Optional.of(context));
        when(provider.analyze(context)).thenReturn(new AgentNarrative("ok", List.of("e"), Confidence.MEDIUM, List.of()));

        orchestrator.onAlertCreated(event);

        org.mockito.ArgumentCaptor<AgentAnalysis> captor = org.mockito.ArgumentCaptor.forClass(AgentAnalysis.class);
        verify(persister).persist(eq(ALERT_ID), captor.capture());
        HistoricalContextSummary summary = captor.getValue().historicalContext();
        assertThat(summary.sampleSize()).isEqualTo(40);
        assertThat(summary.targetBeforeStopProbability()).isEqualByComparingTo("0.65");
    }
}
