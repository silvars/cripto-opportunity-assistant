package com.btcassistant.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Shape esperado da resposta textual do LLM — validada antes de virar {@link AgentNarrative} (SDD §7 DoD). */
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentNarrativeJson(String summary, List<String> evidence, String confidence, List<String> invalidations) {
}
