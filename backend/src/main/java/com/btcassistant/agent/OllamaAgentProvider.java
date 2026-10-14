package com.btcassistant.agent;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

/**
 * Implementação inicial de {@link AgentProvider} (SDD §33). O domínio (orquestrador) não conhece
 * detalhes do Ollama — apenas a interface. Futuro: {@code OpenAIAgentProvider}.
 */
@Component
public class OllamaAgentProvider implements AgentProvider {

    private final AgentProperties properties;
    private final PromptBuilder promptBuilder;
    private final LanguageGuard languageGuard;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public OllamaAgentProvider(AgentProperties properties, PromptBuilder promptBuilder,
                              LanguageGuard languageGuard, ObjectMapper objectMapper) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.languageGuard = languageGuard;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentNarrative analyze(AgentContext context) throws AgentProviderException {
        String prompt = promptBuilder.build(context);
        OllamaGenerateRequest requestPayload = new OllamaGenerateRequest(
                properties.getOllama().getModel(), prompt, false, "json");

        HttpRequest request;
        String responseBody;
        try {
            String requestJson = objectMapper.writeValueAsString(requestPayload);
            request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl() + "/api/generate"))
                    .timeout(properties.getTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AgentProviderException("Ollama retornou HTTP " + response.statusCode(), false, null);
            }
            responseBody = response.body();
        } catch (HttpTimeoutException e) {
            throw new AgentProviderException("Timeout chamando Ollama", true, e);
        } catch (java.io.IOException e) {
            throw new AgentProviderException("Erro de I/O chamando Ollama", false, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentProviderException("Chamada ao Ollama interrompida", false, e);
        }

        return parseAndValidate(responseBody);
    }

    private AgentNarrative parseAndValidate(String responseBody) throws AgentProviderException {
        String rawJson;
        try {
            OllamaGenerateResponse envelope = objectMapper.readValue(responseBody, OllamaGenerateResponse.class);
            rawJson = envelope.response();
        } catch (JacksonException e) {
            throw new AgentProviderException("Resposta do Ollama não é um envelope JSON válido", false, e);
        }

        AgentNarrativeJson parsed;
        try {
            parsed = objectMapper.readValue(rawJson, AgentNarrativeJson.class);
        } catch (JacksonException e) {
            throw new AgentProviderException("Conteúdo do modelo não segue o schema esperado", false, e);
        }

        if (parsed.summary() == null || parsed.evidence() == null || parsed.confidence() == null || parsed.invalidations() == null) {
            throw new AgentProviderException("Resposta do modelo omitiu campos obrigatórios do schema", false, null);
        }

        Confidence confidence;
        try {
            confidence = Confidence.valueOf(parsed.confidence().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AgentProviderException("Valor de confidence fora do schema: " + parsed.confidence(), false, e);
        }

        if (!languageGuard.isCompliant(parsed.summary()) || parsed.evidence().stream().anyMatch(e -> !languageGuard.isCompliant(e))) {
            throw new AgentProviderException("Resposta do modelo violou a regra de linguagem (SDD §40)", false, null);
        }

        return new AgentNarrative(parsed.summary(), List.copyOf(parsed.evidence()), confidence, List.copyOf(parsed.invalidations()));
    }
}
