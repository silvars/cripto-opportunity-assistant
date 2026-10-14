package com.btcassistant.agent;

/** Interpretação textual pedida ao LLM — apenas os campos que o agente realmente decide (SDD §11). */
public record AgentNarrative(String summary, java.util.List<String> evidence, Confidence confidence, java.util.List<String> invalidations) {
}
