package com.btcassistant.opportunity;

import com.btcassistant.risk.RiskAssessment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Insere oportunidades — nunca atualiza (SDD Fase 3 §3). */
@Component
public class OpportunityPersister {

    private static final Logger log = LoggerFactory.getLogger(OpportunityPersister.class);

    private final MarketOpportunityRepository repository;

    public OpportunityPersister(MarketOpportunityRepository repository) {
        this.repository = repository;
    }

    public MarketOpportunityEntity persist(UUID eventId, String symbol, OpportunityResult result, RiskAssessment risk) {
        MarketOpportunityEntity entity = repository.save(MarketOpportunityEntity.from(eventId, symbol, result, risk));
        log.info("opportunity_created symbol={} classification={} score={} riskLevel={}",
                symbol, result.classification(), result.score(), risk.level());
        return entity;
    }
}
