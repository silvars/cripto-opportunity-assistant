package com.btcassistant.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Insere análises — nunca atualiza; reanálise usa {@code analysis_version} incremental (SDD §3). */
@Component
public class AgentAnalysisPersister {

    private static final Logger log = LoggerFactory.getLogger(AgentAnalysisPersister.class);

    private final AgentAnalysisRepository repository;

    public AgentAnalysisPersister(AgentAnalysisRepository repository) {
        this.repository = repository;
    }

    public AgentAnalysisEntity persist(UUID alertId, AgentAnalysis analysis) {
        int nextVersion = repository.findTopByAlertIdOrderByAnalysisVersionDesc(alertId)
                .map(e -> e.getAnalysisVersion() + 1)
                .orElse(1);
        AgentAnalysisEntity entity = repository.save(AgentAnalysisEntity.create(alertId, nextVersion, analysis));
        if (analysis.status() == AnalysisStatus.COMPLETED) {
            log.info("agent_analysis_completed alertId={} analysisVersion={} confidence={}",
                    alertId, nextVersion, analysis.confidence());
        } else {
            log.warn("agent_analysis_failed alertId={} analysisVersion={} status={}",
                    alertId, nextVersion, analysis.status());
        }
        return entity;
    }
}
