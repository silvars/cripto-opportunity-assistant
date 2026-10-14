package com.btcassistant.agent;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.historical.HistoricalContext;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.risk.RiskAssessment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contexto completo pré-buscado para o agente (SDD §36). Diferente do rascunho original (que
 * previa "tools" chamadas sob demanda pelo LLM — {@code getCurrentMarketState}, etc.), esta
 * implementação busca tudo adiantado e inclui no prompt em uma única chamada — ver
 * docs/phases/FASE-5-agent.md §11. {@code opportunity} usa {@link MarketOpportunityEntity}
 * diretamente (já persistida) em vez de recalcular um {@code OpportunityResult} novo.
 */
public record AgentContext(
    InstrumentContext instrument,
    Map<String, Object> market,
    Map<String, Object> features,
    DetectedEvent event,
    MarketOpportunityEntity opportunity,
    RiskAssessment risk,
    Optional<HistoricalContext> historicalContext,
    List<AlertSummary> recentAlerts
) {
}
