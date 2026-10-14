package com.btcassistant.risk;

import java.util.List;

/** Resultado da avaliação de risco — razões sempre auditáveis, nunca uma confiança opaca (SDD §25/§26). */
public record RiskAssessment(RiskLevel level, List<String> reasons) {
}
