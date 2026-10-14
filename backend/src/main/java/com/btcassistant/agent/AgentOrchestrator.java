package com.btcassistant.agent;

import com.btcassistant.alert.AlertCreatedEvent;
import com.btcassistant.historical.HistoricalContext;
import com.btcassistant.historical.HistoricalProperties;
import com.btcassistant.historical.OutcomeStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Optional;

/**
 * O LLM não roda a cada minuto (SDD §32) — só quando um alerta novo (score >= trigger-threshold)
 * é de fato criado. Nunca bloqueia o pipeline quantitativo: qualquer falha do provider vira um
 * {@link AgentAnalysis} com status FAILED/TIMEOUT, nunca uma exceção propagada (SDD §34/§35).
 */
@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final AgentProperties properties;
    private final HistoricalProperties historicalProperties;
    private final AgentContextBuilder contextBuilder;
    private final AgentProvider provider;
    private final AgentAnalysisPersister persister;

    public AgentOrchestrator(AgentProperties properties, HistoricalProperties historicalProperties,
                             AgentContextBuilder contextBuilder, AgentProvider provider, AgentAnalysisPersister persister) {
        this.properties = properties;
        this.historicalProperties = historicalProperties;
        this.contextBuilder = contextBuilder;
        this.provider = provider;
        this.persister = persister;
    }

    @EventListener
    public void onAlertCreated(AlertCreatedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        if (event.alert().getScore().compareTo(properties.getTriggerThreshold()) < 0) {
            return;
        }

        Optional<AgentContext> context = contextBuilder.build(event);
        if (context.isEmpty()) {
            log.warn("agent_skipped alertId={} reason=missing-context", event.alert().getAlertId());
            return;
        }

        log.info("agent_started alertId={} symbol={}", event.alert().getAlertId(), event.alert().getSymbol());
        AgentAnalysis analysis = callWithRetry(context.get());
        persister.persist(event.alert().getAlertId(), analysis);
    }

    private AgentAnalysis callWithRetry(AgentContext context) {
        AgentProviderException lastError = null;
        int attempts = properties.getMaxRetries() + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                AgentNarrative narrative = provider.analyze(context);
                return completed(context, narrative);
            } catch (AgentProviderException e) {
                lastError = e;
                log.warn("agent_attempt_failed attempt={} timeout={} message={}", attempt, e.isTimeout(), e.getMessage());
            }
        }
        return failed(context, lastError);
    }

    private AgentAnalysis completed(AgentContext context, AgentNarrative narrative) {
        return new AgentAnalysis(
                context.opportunity().getClassification(),
                context.opportunity().getDirection(),
                context.opportunity().getScore(),
                narrative.confidence(),
                narrative.summary(),
                narrative.evidence(),
                summarize(context.historicalContext()),
                context.risk(),
                narrative.invalidations(),
                properties.getVersion(),
                properties.getPromptVersion(),
                AnalysisStatus.COMPLETED);
    }

    private AgentAnalysis failed(AgentContext context, AgentProviderException error) {
        AnalysisStatus status = error != null && error.isTimeout() ? AnalysisStatus.TIMEOUT : AnalysisStatus.FAILED;
        return new AgentAnalysis(
                context.opportunity().getClassification(),
                context.opportunity().getDirection(),
                context.opportunity().getScore(),
                Confidence.LOW,
                "Análise de IA indisponível no momento — pipeline quantitativo não é afetado.",
                List.of(),
                summarize(context.historicalContext()),
                context.risk(),
                List.of(),
                properties.getVersion(),
                properties.getPromptVersion(),
                status);
    }

    private HistoricalContextSummary summarize(Optional<HistoricalContext> historicalContext) {
        if (historicalContext.isEmpty() || historicalContext.get().outcomesByHorizon().isEmpty()) {
            return null;
        }
        HistoricalContext context = historicalContext.get();
        OutcomeStats canonical = context.outcomesByHorizon().get(historicalProperties.getCanonicalHorizon());
        BigDecimal probability = canonical != null ? canonical.targetBeforeStopProbability() : averageProbability(context);
        return new HistoricalContextSummary(context.sampleSize(), probability);
    }

    private static BigDecimal averageProbability(HistoricalContext context) {
        BigDecimal sum = BigDecimal.ZERO;
        for (OutcomeStats stats : context.outcomesByHorizon().values()) {
            sum = sum.add(stats.targetBeforeStopProbability());
        }
        return sum.divide(BigDecimal.valueOf(context.outcomesByHorizon().size()), MathContext.DECIMAL64);
    }
}
