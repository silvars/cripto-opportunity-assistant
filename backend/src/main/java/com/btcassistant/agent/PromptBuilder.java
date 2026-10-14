package com.btcassistant.agent;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Monta o prompt com TODO o contexto relevante já buscado adiantado (ver {@link AgentContext} —
 * não há chamadas de "tool" feitas pelo LLM nesta versão). Pede explicitamente apenas os quatro
 * campos que o agente realmente decide (SDD Fase 5 §11).
 */
@Component
public class PromptBuilder {

    private final ObjectMapper objectMapper;

    public PromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(AgentContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instrument", context.instrument());
        payload.put("market", context.market());
        payload.put("features", context.features());
        payload.put("event", Map.of(
                "eventType", context.event().eventType(),
                "eventTime", context.event().eventTime().toString(),
                "price", context.event().price(),
                "magnitude", context.event().magnitude()));
        payload.put("opportunity", Map.of(
                "classification", context.opportunity().getClassification(),
                "direction", context.opportunity().getDirection(),
                "score", context.opportunity().getScore(),
                "evidenceQuality", context.opportunity().getEvidenceQuality()));
        payload.put("risk", Map.of("level", context.risk().level(), "reasons", context.risk().reasons()));
        payload.put("historicalContext", context.historicalContext()
                .map(h -> (Object) Map.of("sampleSize", h.sampleSize(), "outcomesByHorizon", h.outcomesByHorizon()))
                .orElse(Map.of("status", "not_available")));
        payload.put("recentAlerts", context.recentAlerts());

        String contextJson = objectMapper.writeValueAsString(payload);

        return """
                Você é um analista de mercado que interpreta evidências quantitativas JÁ CALCULADAS
                pelo sistema para o instrumento %s. Você NUNCA calcula métricas, preços ou
                probabilidades — apenas interpreta os dados fornecidos abaixo.

                Regras obrigatórias:
                - Nunca invente números, preços, volumes ou probabilidades que não estejam no contexto.
                - Se "historicalContext" tiver "status": "not_available", declare explicitamente que o
                  contexto histórico está indisponível — nunca invente um tamanho de amostra.
                - Nunca recomende ação imediata como "compre agora" ou "venda agora". Prefira linguagem
                  como "setup que merece avaliação" ou "contexto favorável a uma possível entrada".
                - Nunca execute ou simule execução de ordens.

                Contexto (JSON):
                %s

                Responda APENAS com um objeto JSON com exatamente estas chaves:
                {"summary": string, "evidence": [string, ...], "confidence": "LOW"|"MEDIUM"|"HIGH", "invalidations": [string, ...]}
                """.formatted(context.instrument().symbol(), contextJson);
    }
}
