package com.btcassistant.risk;

/** Nível de risco/contexto da oportunidade (SDD §25). Nunca "inventa" confiança — dados insuficientes retornam UNKNOWN. */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN
}
