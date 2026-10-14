package com.btcassistant.alert;

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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cobre a regra central do DoD: "durante um movimento prolongado, não gera alertas duplicados" (SDD §41/§43). */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository repository;

    private final AlertProperties properties = new AlertProperties();
    private AlertService service;

    private static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private MarketOpportunityEntity opportunity(Classification classification) {
        OpportunityResult result = new OpportunityResult(Direction.BUY, classification, new BigDecimal("70"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.5"), EvidenceQuality.LIMITED, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        return MarketOpportunityEntity.from(UUID.randomUUID(), "BTCUSDT", result, risk);
    }

    private static DetectedEvent detectedEvent() {
        return new DetectedEvent("BTCUSDT", EventTypes.RAPID_RISE, EVENT_TIME, new BigDecimal("100"),
                Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
    }

    private static AlertEntity existingAlert(AlertStatus status, Instant createdAt, Instant expiresAt) {
        AlertEntity existing = mock(AlertEntity.class);
        lenient().when(existing.getStatus()).thenReturn(status);
        lenient().when(existing.getCreatedAt()).thenReturn(createdAt);
        lenient().when(existing.getExpiresAt()).thenReturn(expiresAt);
        lenient().when(existing.getSymbol()).thenReturn("BTCUSDT");
        lenient().when(existing.getDirection()).thenReturn(Direction.BUY);
        lenient().when(existing.getAlertId()).thenReturn(UUID.randomUUID());
        return existing;
    }

    @Test
    void neverCreatesAlertWhenClassificationIsNone() {
        service = new AlertService(repository, properties);

        Optional<AlertEntity> result = service.process(opportunity(Classification.NONE), detectedEvent());

        assertThat(result).isEmpty();
        verify(repository, never()).findTopBySymbolAndDirectionOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void createsNewAlertWhenNoneExistsYet() {
        service = new AlertService(repository, properties);
        when(repository.findTopBySymbolAndDirectionOrderByCreatedAtDesc("BTCUSDT", Direction.BUY)).thenReturn(Optional.empty());
        when(repository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<AlertEntity> result = service.process(opportunity(Classification.SETUP), detectedEvent());

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(AlertStatus.ACTIVE);
    }

    @Test
    void suppressesNewAlertWhenAnActiveOneAlreadyExists() {
        service = new AlertService(repository, properties);
        Instant now = Instant.now();
        AlertEntity existing = existingAlert(AlertStatus.ACTIVE, now.minus(Duration.ofMinutes(5)), now.plus(Duration.ofMinutes(10)));
        when(repository.findTopBySymbolAndDirectionOrderByCreatedAtDesc("BTCUSDT", Direction.BUY)).thenReturn(Optional.of(existing));

        Optional<AlertEntity> result = service.process(opportunity(Classification.SETUP), detectedEvent());

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void expiresStaleActiveAlertAndCreatesNewOneWhenDedupWindowAlsoPassed() {
        service = new AlertService(repository, properties);
        Instant now = Instant.now();
        // criado há 20min, expirou há 5min — e dedup-window (15min) também já passou
        AlertEntity existing = existingAlert(AlertStatus.ACTIVE, now.minus(Duration.ofMinutes(20)), now.minus(Duration.ofMinutes(5)));
        when(repository.findTopBySymbolAndDirectionOrderByCreatedAtDesc("BTCUSDT", Direction.BUY)).thenReturn(Optional.of(existing));
        when(repository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<AlertEntity> result = service.process(opportunity(Classification.SETUP), detectedEvent());

        assertThat(result).isPresent();
        verify(existing).transitionTo(AlertStatus.EXPIRED);
        verify(repository, times(2)).save(any());
    }

    @Test
    void expiresStaleActiveAlertButStillSuppressesWithinDedupWindow() {
        service = new AlertService(repository, properties);
        Instant now = Instant.now();
        // criado há 10min, expirou há 1min (cooldown mais curto que dedup-window) — mas ainda
        // dentro da janela de dedup de 15min
        AlertEntity existing = existingAlert(AlertStatus.ACTIVE, now.minus(Duration.ofMinutes(10)), now.minus(Duration.ofMinutes(1)));
        when(repository.findTopBySymbolAndDirectionOrderByCreatedAtDesc("BTCUSDT", Direction.BUY)).thenReturn(Optional.of(existing));

        Optional<AlertEntity> result = service.process(opportunity(Classification.SETUP), detectedEvent());

        assertThat(result).isEmpty();
        verify(existing).transitionTo(AlertStatus.EXPIRED);
        verify(repository, times(1)).save(any());
    }

    @Test
    void suppressesWhenMostRecentAlertIsAlreadyExpiredButStillWithinDedupWindow() {
        service = new AlertService(repository, properties);
        Instant now = Instant.now();
        AlertEntity existing = existingAlert(AlertStatus.EXPIRED, now.minus(Duration.ofMinutes(5)), now.minus(Duration.ofMinutes(1)));
        when(repository.findTopBySymbolAndDirectionOrderByCreatedAtDesc("BTCUSDT", Direction.BUY)).thenReturn(Optional.of(existing));

        Optional<AlertEntity> result = service.process(opportunity(Classification.SETUP), detectedEvent());

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void createsNewAlertWhenMostRecentIsExpiredAndDedupWindowPassed() {
        service = new AlertService(repository, properties);
        Instant now = Instant.now();
        AlertEntity existing = existingAlert(AlertStatus.EXPIRED, now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(20)));
        when(repository.findTopBySymbolAndDirectionOrderByCreatedAtDesc("BTCUSDT", Direction.BUY)).thenReturn(Optional.of(existing));
        when(repository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<AlertEntity> result = service.process(opportunity(Classification.SETUP), detectedEvent());

        assertThat(result).isPresent();
        verify(repository, times(1)).save(any());
    }
}
