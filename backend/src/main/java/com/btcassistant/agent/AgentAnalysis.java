package com.btcassistant.agent;

import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.risk.RiskAssessment;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saída do agente (SDD §38). {@code classification}/{@code direction}/{@code score}/{@code risk}/
 * {@code historicalContext} são sempre ecoados do que já foi calculado deterministicamente
 * (SDD §39 "não substitui cálculos determinísticos") — o agente nunca os decide; apenas
 * {@code summary}/{@code evidence}/{@code confidence}/{@code invalidations} vêm do LLM
 * (ver docs/phases/FASE-5-agent.md §11).
 */
public record AgentAnalysis(
    Classification classification,
    Direction direction,
    BigDecimal score,
    Confidence confidence,
    String summary,
    List<String> evidence,
    HistoricalContextSummary historicalContext,
    RiskAssessment risk,
    List<String> invalidations,
    String agentVersion,
    String promptVersion,
    AnalysisStatus status
) {
}
