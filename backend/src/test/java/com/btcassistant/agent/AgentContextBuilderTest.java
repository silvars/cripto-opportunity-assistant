package com.btcassistant.agent;

import com.btcassistant.alert.AlertCreatedEvent;
import com.btcassistant.alert.AlertEntity;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.feature.FeatureNames;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.feature.MarketFeatureEntity;
import com.btcassistant.feature.MarketFeatureRepository;
import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import com.btcassistant.market.MarketProperties;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.EvidenceQuality;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.MarketOpportunityRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentContextBuilderTest {

    private static final String SYMBOL = "BTCUSDT";
    private static final String INTERVAL = "1m";
    private static final Instant EVENT_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private CandleRepository candleRepository;
    @Mock
    private MarketFeatureRepository featureRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private MarketOpportunityRepository opportunityRepository;

    private final FeatureProperties featureProperties = new FeatureProperties();
    private MarketProperties marketProperties;
    private AgentContextBuilder builder;

    @BeforeEach
    void setUp() {
        MarketProperties.InstrumentConfig config = new MarketProperties.InstrumentConfig();
        config.setSymbol(SYMBOL);
        config.setBaseAsset("BTC");
        config.setQuoteAsset("USDT");
        config.setExchange("BINANCE");
        config.setInterval(INTERVAL);
        config.setEnabled(true);
        marketProperties = new MarketProperties();
        marketProperties.setInstruments(List.of(config));

        builder = new AgentContextBuilder(marketProperties, candleRepository, featureRepository, featureProperties,
                alertRepository, opportunityRepository);
    }

    private MarketOpportunityEntity opportunity() {
        OpportunityResult result = new OpportunityResult(Direction.BUY, Classification.SETUP, new BigDecimal("80"),
                java.util.Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.8"), EvidenceQuality.FULL, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        return MarketOpportunityEntity.from(UUID.randomUUID(), SYMBOL, result, risk);
    }

    private AlertEntity alert(MarketOpportunityEntity opportunity) {
        return AlertEntity.from(opportunity, detectedEvent(), EVENT_TIME.plus(Duration.ofMinutes(15)));
    }

    private static DetectedEvent detectedEvent() {
        return new DetectedEvent(SYMBOL, EventTypes.RAPID_RISE, EVENT_TIME, new BigDecimal("50000"),
                java.util.Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
    }

    @Test
    void returnsEmptyWhenInstrumentIsNotConfigured() {
        AlertEntity alert = AlertEntity.from(opportunity(),
                new DetectedEvent("ETHUSDT", EventTypes.RAPID_RISE, EVENT_TIME, new BigDecimal("3000"),
                        java.util.Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0"),
                EVENT_TIME.plus(Duration.ofMinutes(15)));
        AlertCreatedEvent event = new AlertCreatedEvent(alert, detectedEvent(), Optional.empty());

        assertThat(builder.build(event)).isEmpty();
    }

    @Test
    void returnsEmptyWhenOpportunityNoLongerExists() {
        MarketOpportunityEntity opportunity = opportunity();
        AlertEntity alert = alert(opportunity);
        when(opportunityRepository.findById(opportunity.getOpportunityId())).thenReturn(Optional.empty());
        AlertCreatedEvent event = new AlertCreatedEvent(alert, detectedEvent(), Optional.empty());

        assertThat(builder.build(event)).isEmpty();
    }

    @Test
    void assemblesFullContextExcludingCurrentAlertFromRecentAlerts() {
        MarketOpportunityEntity opportunity = opportunity();
        UUID opportunityId = opportunity.getOpportunityId();
        AlertEntity currentAlert = alert(opportunity);
        AlertEntity otherAlert = alert(opportunity());
        when(opportunityRepository.findById(opportunityId)).thenReturn(Optional.of(opportunity));

        CandleEntity candle = CandleEntity.from(new Candle(SYMBOL, INTERVAL, EVENT_TIME, EVENT_TIME.plus(Duration.ofMinutes(1)),
                new BigDecimal("49000"), new BigDecimal("50500"), new BigDecimal("48900"), new BigDecimal("50000"),
                new BigDecimal("10"), new BigDecimal("500000"), 100, "BINANCE"));
        when(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc(SYMBOL, INTERVAL)).thenReturn(Optional.of(candle));

        FeatureSnapshot snapshot = new FeatureSnapshot(SYMBOL, INTERVAL, EVENT_TIME, "1.0",
                java.util.Map.of(FeatureNames.MOMENTUM_15M, new BigDecimal("0.03")));
        when(featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(SYMBOL, INTERVAL, EVENT_TIME, featureProperties.getVersion()))
                .thenReturn(Optional.of(MarketFeatureEntity.from(snapshot)));

        when(alertRepository.findTop50BySymbolOrderByCreatedAtDesc(SYMBOL)).thenReturn(List.of(currentAlert, otherAlert));

        AlertCreatedEvent event = new AlertCreatedEvent(currentAlert, detectedEvent(), Optional.empty());

        Optional<AgentContext> result = builder.build(event);

        assertThat(result).isPresent();
        AgentContext context = result.get();
        assertThat(context.instrument().symbol()).isEqualTo(SYMBOL);
        assertThat(context.market()).containsEntry("latestClose", new BigDecimal("50000"));
        assertThat(context.features()).containsEntry(FeatureNames.MOMENTUM_15M, new BigDecimal("0.03"));
        assertThat(context.opportunity().getOpportunityId()).isEqualTo(opportunityId);
        assertThat(context.recentAlerts())
                .hasSize(1)
                .allMatch(summary -> !summary.alertId().equals(currentAlert.getAlertId()));
    }
}
