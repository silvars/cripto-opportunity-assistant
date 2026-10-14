package com.btcassistant.pipeline;

import com.btcassistant.alert.AlertService;
import com.btcassistant.event.MarketEventPersistedEvent;
import com.btcassistant.feature.FeatureProperties;
import com.btcassistant.feature.FeatureSnapshot;
import com.btcassistant.feature.MarketFeatureEntity;
import com.btcassistant.feature.MarketFeatureRepository;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketProperties;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.OpportunityEngine;
import com.btcassistant.opportunity.OpportunityPersister;
import com.btcassistant.opportunity.OpportunityResult;
import com.btcassistant.risk.RiskAssessment;
import com.btcassistant.risk.RiskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fio condutor do pipeline quantitativo market → feature → event → opportunity → risk → alert
 * (ver .github/copilot-instructions.md). Vive fora de {@code opportunity}/{@code alert} de
 * propósito: {@code alert} já depende de {@code opportunity} (FK opportunity_id); se o
 * orquestrador vivesse dentro de {@code opportunity} e chamasse {@code AlertService}, criaria uma
 * dependência circular entre os dois módulos (SDD Fase 3 §11).
 *
 * <p>Evento persistido → Opportunity Engine → Risk Engine → persiste a oportunidade (sempre,
 * mesmo {@code NONE} — "uma linha por avaliação", SDD Fase 3 §3) → se classificação != NONE,
 * aciona o {@link AlertService} (SDD Fase 3 §6).</p>
 */
@Component
public class OpportunityPipeline {

    private static final Logger log = LoggerFactory.getLogger(OpportunityPipeline.class);

    private final MarketProperties marketProperties;
    private final MarketFeatureRepository featureRepository;
    private final FeatureProperties featureProperties;
    private final OpportunityEngine opportunityEngine;
    private final RiskEngine riskEngine;
    private final OpportunityPersister opportunityPersister;
    private final AlertService alertService;

    public OpportunityPipeline(MarketProperties marketProperties,
                               MarketFeatureRepository featureRepository,
                               FeatureProperties featureProperties,
                               OpportunityEngine opportunityEngine,
                               RiskEngine riskEngine,
                               OpportunityPersister opportunityPersister,
                               AlertService alertService) {
        this.marketProperties = marketProperties;
        this.featureRepository = featureRepository;
        this.featureProperties = featureProperties;
        this.opportunityEngine = opportunityEngine;
        this.riskEngine = riskEngine;
        this.opportunityPersister = opportunityPersister;
        this.alertService = alertService;
    }

    @EventListener
    public void onMarketEventPersisted(MarketEventPersistedEvent persistedEvent) {
        var event = persistedEvent.event();
        Instrument instrument = marketProperties.findEnabledBySymbol(event.symbol());
        if (instrument == null) {
            return;
        }

        Optional<MarketFeatureEntity> featureEntity = featureRepository
                .findBySymbolAndIntervalAndCandleOpenTimeAndFeatureVersion(
                        event.symbol(), instrument.interval(), event.eventTime(), featureProperties.getVersion());
        if (featureEntity.isEmpty()) {
            log.warn("opportunity_evaluation_skipped symbol={} eventTime={} reason=missing-feature-snapshot",
                    event.symbol(), event.eventTime());
            return;
        }

        FeatureSnapshot features = featureEntity.get().toSnapshot();
        OpportunityResult result = opportunityEngine.evaluate(event, features, Optional.empty());
        RiskAssessment risk = riskEngine.assess(event, features);

        MarketOpportunityEntity opportunity = opportunityPersister.persist(persistedEvent.eventId(), event.symbol(), result, risk);

        if (result.classification() != Classification.NONE) {
            alertService.process(opportunity, event);
        }
    }
}
