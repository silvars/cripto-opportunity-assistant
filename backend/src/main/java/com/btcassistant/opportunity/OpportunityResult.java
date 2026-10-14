package com.btcassistant.opportunity;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado do Opportunity Engine (SDD Fase 3 §4). {@code scoreComponents} contém apenas os
 * componentes efetivamente calculados (em pontos, na escala do peso configurado do componente) —
 * um componente ausente (UNKNOWN) é omitido do mapa, nunca aparece como {@code null}/zero
 * (ver docs/phases/FASE-3-opportunity-risk.md §11: {@code Map<String,BigDecimal>} não pode
 * representar o marcador "UNKNOWN" do rascunho original do SDD inline com valores numéricos).
 * {@code engineVersion} foi adicionado ao rascunho original — necessário para popular a coluna
 * {@code engine_version} de {@code opportunities}, não coberta pelo contrato original do SDD.
 */
public record OpportunityResult(
    Direction direction,
    Classification classification,
    BigDecimal score,
    Map<String, BigDecimal> scoreComponents,
    BigDecimal availableEvidenceWeight,
    EvidenceQuality evidenceQuality,
    String configVersion,
    String engineVersion
) {
}
