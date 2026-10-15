package com.btcassistant.research;

import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.historical.HistoricalOutcomeCalculator;
import com.btcassistant.historical.HistoricalOutcomeResult;
import com.btcassistant.market.MarketProperties;
import com.btcassistant.opportunity.ExecutionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Preenche apenas horizontes que já decorreram, nunca sobrescreve um valor já calculado (SDD §58). */
@ExtendWith(MockitoExtension.class)
class AlertEvaluatorTest {

    private static final Instant ALERT_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertEvaluationRepository evaluationRepository;
    @Mock
    private HistoricalOutcomeCalculator outcomeCalculator;

    private MarketProperties marketProperties;
    private ResearchProperties properties;
    private AlertEvaluator evaluator;

    @BeforeEach
    void setUp() {
        MarketProperties.InstrumentConfig config = new MarketProperties.InstrumentConfig();
        config.setSymbol("BTCUSDT");
        config.setBaseAsset("BTC");
        config.setQuoteAsset("USDT");
        config.setExchange("BINANCE");
        config.setInterval("1m");
        config.setEnabled(true);
        marketProperties = new MarketProperties();
        marketProperties.setInstruments(List.of(config));

        properties = new ResearchProperties();
        properties.getEvaluation().setHorizons(List.of("5m", "15m"));
        properties.getEvaluation().setCanonicalHorizon("15m");

        evaluator = new AlertEvaluator(alertRepository, evaluationRepository, outcomeCalculator, marketProperties, properties);
        lenient().when(evaluationRepository.save(any(AlertEvaluationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AlertEntity alert(ExecutionMode executionMode) {
        AlertEntity alert = mock(AlertEntity.class);
        UUID alertId = UUID.randomUUID();
        lenient().when(alert.getAlertId()).thenReturn(alertId);
        lenient().when(alert.getSymbol()).thenReturn("BTCUSDT");
        lenient().when(alert.getCreatedAt()).thenReturn(ALERT_TIME);
        lenient().when(alert.getPriceAtAlert()).thenReturn(new BigDecimal("50000"));
        lenient().when(alert.getExecutionMode()).thenReturn(executionMode);
        return alert;
    }

    private HistoricalOutcomeResult outcomeFor(UUID alertId, String horizon, BigDecimal futureReturn) {
        return new HistoricalOutcomeResult(alertId, horizon, futureReturn, new BigDecimal("0.02"), new BigDecimal("-0.01"),
                null, null, true, ALERT_TIME.plus(Duration.ofMinutes(15)), "1.0", "1.0");
    }

    @Test
    void fillsOnlyHorizonsThatHaveAlreadyElapsed() {
        AlertEntity alert = alert(ExecutionMode.LIVE);
        UUID alertId = alert.getAlertId();
        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(evaluationRepository.findById(alertId)).thenReturn(Optional.empty());
        when(outcomeCalculator.calculate(eq(alertId), eq("BTCUSDT"), eq("1m"), eq(ALERT_TIME), any(), eq("5m")))
                .thenReturn(Optional.of(outcomeFor(alertId, "5m", new BigDecimal("0.01"))));
        when(outcomeCalculator.calculate(eq(alertId), eq("BTCUSDT"), eq("1m"), eq(ALERT_TIME), any(), eq("15m")))
                .thenReturn(Optional.empty());

        evaluator.evaluatePendingAlerts(ALERT_TIME.plus(Duration.ofDays(1)));

        var captor = org.mockito.ArgumentCaptor.forClass(AlertEvaluationEntity.class);
        verify(evaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getHorizonReturn("5m")).isEqualByComparingTo("0.01");
        assertThat(captor.getValue().getHorizonReturn("15m")).isNull();
        assertThat(captor.getValue().getMfe()).isNull(); // canônico (15m) ainda não resolvido
    }

    @Test
    void setsOutcomeSummaryOnlyWhenCanonicalHorizonResolves() {
        AlertEntity alert = alert(ExecutionMode.LIVE);
        UUID alertId = alert.getAlertId();
        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(evaluationRepository.findById(alertId)).thenReturn(Optional.empty());
        when(outcomeCalculator.calculate(eq(alertId), eq("BTCUSDT"), eq("1m"), eq(ALERT_TIME), any(), eq("5m")))
                .thenReturn(Optional.of(outcomeFor(alertId, "5m", new BigDecimal("0.01"))));
        when(outcomeCalculator.calculate(eq(alertId), eq("BTCUSDT"), eq("1m"), eq(ALERT_TIME), any(), eq("15m")))
                .thenReturn(Optional.of(outcomeFor(alertId, "15m", new BigDecimal("0.02"))));

        evaluator.evaluatePendingAlerts(ALERT_TIME.plus(Duration.ofDays(1)));

        var captor = org.mockito.ArgumentCaptor.forClass(AlertEvaluationEntity.class);
        verify(evaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getMfe()).isEqualByComparingTo("0.02");
        assertThat(captor.getValue().getTargetBeforeStop()).isTrue();
    }

    @Test
    void neverOverwritesAnAlreadyFilledHorizon() {
        AlertEntity alert = alert(ExecutionMode.LIVE);
        UUID alertId = alert.getAlertId();
        AlertEvaluationEntity existing = AlertEvaluationEntity.create(alertId, ExecutionMode.LIVE);
        existing.setHorizonReturn("5m", new BigDecimal("0.99"));

        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(evaluationRepository.findById(alertId)).thenReturn(Optional.of(existing));
        when(outcomeCalculator.calculate(eq(alertId), eq("BTCUSDT"), eq("1m"), eq(ALERT_TIME), any(), eq("15m")))
                .thenReturn(Optional.of(outcomeFor(alertId, "15m", new BigDecimal("0.02"))));

        evaluator.evaluatePendingAlerts(ALERT_TIME.plus(Duration.ofDays(1)));

        assertThat(existing.getHorizonReturn("5m")).isEqualByComparingTo("0.99");
        verify(outcomeCalculator, never()).calculate(any(), any(), any(), any(), any(), eq("5m"));
    }

    @Test
    void skipsAlertsThatAreNotYetBeforeAsOf() {
        AlertEntity alert = alert(ExecutionMode.LIVE);
        when(alertRepository.findAll()).thenReturn(List.of(alert));

        evaluator.evaluatePendingAlerts(ALERT_TIME.minus(Duration.ofMinutes(1)));

        verify(evaluationRepository, never()).findById(any());
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void skipsBacktestAlerts() {
        AlertEntity backtestAlert = alert(ExecutionMode.BACKTEST);
        when(alertRepository.findAll()).thenReturn(List.of(backtestAlert));

        evaluator.evaluatePendingAlerts(ALERT_TIME.plus(Duration.ofDays(1)));

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void skipsFullyEvaluatedAlertsWithoutCallingCalculator() {
        AlertEntity alert = alert(ExecutionMode.LIVE);
        UUID alertId = alert.getAlertId();
        AlertEvaluationEntity existing = AlertEvaluationEntity.create(alertId, ExecutionMode.LIVE);
        existing.setHorizonReturn("5m", new BigDecimal("0.01"));
        existing.setHorizonReturn("15m", new BigDecimal("0.02"));
        existing.setOutcomeSummary(new BigDecimal("0.02"), new BigDecimal("-0.01"), true);

        when(alertRepository.findAll()).thenReturn(List.of(alert));
        when(evaluationRepository.findById(alertId)).thenReturn(Optional.of(existing));

        evaluator.evaluatePendingAlerts(ALERT_TIME.plus(Duration.ofDays(1)));

        verify(outcomeCalculator, never()).calculate(any(), any(), any(), any(), any(), any());
        verify(evaluationRepository, never()).save(any());
    }
}
