package com.btcassistant.agent;

import com.btcassistant.alert.AlertCreatedEvent;
import com.btcassistant.alert.AlertRepository;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.MarketFeatureRepository;
import com.btcassistant.market.CandleRepository;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketProperties;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.MarketOpportunityRepository;
import com.btcassistant.risk.RiskAssessment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Busca TODO o contexto necessário adiantado — ver nota de design em {@link AgentContext}. */
@Component
public class AgentContextBuilder {

    private static final int RECENT_ALERTS_LIMIT = 5;

    private final MarketProperties marketProperties;
    private final CandleRepository candleRepository;
    private final MarketFeatureRepository featureRepository;
    private final FeatureProperties featureProperties;
    private final AlertRepository alertRepository;
    private final MarketOpportunityRepository opportunityRepository;

    public AgentContextBuilder(MarketProperties marketProperties, CandleRepository candleRepository,
                              MarketFeatureRepository featureRepository, FeatureProperties featureProperties,
                              AlertRepository alertRepository, MarketOpportunityRepository opportunityRepository) {
        this.marketProperties = marketProperties;
        this.candleRepository = candleRepository;
        this.featureRepository = featureRepository;
        this.featureProperties = featureProperties;
        this.alertRepository = alertRepository;
        this.opportunityRepository = opportunityRepository;
    }

    public Optional<AgentContext> build(AlertCreatedEvent event) {
        var alert = event.alert();
        Instrument instrument = marketProperties.findEnabledBySymbol(alert.getSymbol());
        if (instrument == null) {
            return Optional.empty();
        }
        MarketOpportunityEntity opportunity = opportunityRepository.findById(alert.getOpportunityId()).orElse(null);
        if (opportunity == null) {
            return Optional.empty();
        }
        RiskAssessment risk = new RiskAssessment(opportunity.getRiskLevel(), opportunity.getRiskReasons());

        InstrumentContext instrumentContext = new InstrumentContext(
                instrument.symbol(), instrument.baseAsset(), instrument.quoteAsset(), instrument.exchange(), instrument.interval());

        Map<String, Object> market = new LinkedHashMap<>();
        candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc(alert.getSymbol(), instrument.interval())
                .ifPresent(candle -> {
                    market.put("latestClose", candle.getClose());
                    market.put("latestOpenTime", candle.getOpenTime().toString());
                });

        Map<String, Object> features = featureRepository.findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
                        alert.getSymbol(), instrument.interval(), event.event().eventTime(), featureProperties.getVersion())
                .map(entity -> (Map<String, Object>) new LinkedHashMap<String, Object>(entity.toSnapshot().values()))
                .orElse(Map.of());

        List<AlertSummary> recentAlerts = alertRepository.findTop50BySymbolOrderByCreatedAtDesc(alert.getSymbol())
                .stream()
                .filter(a -> !a.getAlertId().equals(alert.getAlertId()))
                .map(a -> new AlertSummary(a.getAlertId(), a.getDirection().name(), a.getClassification().name(), a.getScore(), a.getCreatedAt()))
                .limit(RECENT_ALERTS_LIMIT)
                .toList();

        return Optional.of(new AgentContext(instrumentContext, market, features, event.event(), opportunity, risk,
                event.historicalContext(), recentAlerts));
    }
}
