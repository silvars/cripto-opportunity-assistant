package com.btcassistant.agent;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.event.EventTypes;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.Direction;
import com.btcassistant.opportunity.EvidenceQuality;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import com.btcassistant.opportunity.OpportunityResult;
import com.btcassistant.risk.RiskAssessment;
import com.btcassistant.risk.RiskLevel;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes reais ponta-a-ponta do cliente HTTP (sem mockar a camada de rede): sobe um
 * {@link HttpServer} local simulando o endpoint {@code /api/generate} do Ollama. Cobre o DoD da
 * Fase 5: schema válido, timeout, resposta malformada e violação da regra de linguagem (SDD §40).
 */
class OllamaAgentProviderTest {

    private HttpServer server;
    private int port;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LanguageGuard languageGuard = new LanguageGuard();
    private final PromptBuilder promptBuilder = new PromptBuilder(objectMapper);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private AgentProperties properties(Duration timeout) {
        AgentProperties properties = new AgentProperties();
        properties.getOllama().setBaseUrl("http://localhost:" + port);
        properties.setTimeout(timeout);
        return properties;
    }

    private OllamaAgentProvider provider(Duration timeout) {
        return new OllamaAgentProvider(properties(timeout), promptBuilder, languageGuard, objectMapper);
    }

    private AgentContext context() {
        OpportunityResult result = new OpportunityResult(Direction.BUY, Classification.SETUP, new BigDecimal("80"),
                Map.of("eventStrength", new BigDecimal("20")), new BigDecimal("0.8"), EvidenceQuality.FULL, "1.0", "1.0");
        RiskAssessment risk = new RiskAssessment(RiskLevel.LOW, List.of("no_significant_risk_signals"));
        MarketOpportunityEntity opportunity = MarketOpportunityEntity.from(UUID.randomUUID(), "BTCUSDT", result, risk);
        DetectedEvent event = new DetectedEvent("BTCUSDT", EventTypes.RAPID_RISE, Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("50000"), Map.of(), new BigDecimal("0.03"), Duration.ofMinutes(15), "1.0");
        InstrumentContext instrument = new InstrumentContext("BTCUSDT", "BTC", "USDT", "BINANCE", "1m");
        return new AgentContext(instrument, Map.of("price", new BigDecimal("50000")), Map.of("momentum_15m", new BigDecimal("0.03")),
                event, opportunity, risk, Optional.empty(), List.of());
    }

    private void respondWith(String innerJson) throws IOException {
        String envelope = objectMapper.writeValueAsString(Map.of("response", innerJson));
        byte[] bytes = envelope.getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/generate", exchange -> {
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @Test
    void parsesValidSchemaIntoNarrative() throws Exception {
        String inner = objectMapper.writeValueAsString(Map.of(
                "summary", "Setup de compra que merece avaliação.",
                "evidence", List.of("momentum positivo", "volume acima da média"),
                "confidence", "MEDIUM",
                "invalidations", List.of("reversão abaixo de 49000")));
        respondWith(inner);

        AgentNarrative narrative = provider(Duration.ofSeconds(5)).analyze(context());

        assertThat(narrative.summary()).isEqualTo("Setup de compra que merece avaliação.");
        assertThat(narrative.evidence()).containsExactly("momentum positivo", "volume acima da média");
        assertThat(narrative.confidence()).isEqualTo(Confidence.MEDIUM);
        assertThat(narrative.invalidations()).containsExactly("reversão abaixo de 49000");
    }

    @Test
    void rejectsResponseMissingRequiredField() throws Exception {
        String inner = objectMapper.writeValueAsString(Map.of(
                "summary", "Setup válido.",
                "confidence", "LOW",
                "invalidations", List.of()));
        respondWith(inner);

        assertThatThrownBy(() -> provider(Duration.ofSeconds(5)).analyze(context()))
                .isInstanceOf(AgentProviderException.class)
                .hasMessageContaining("omitiu campos obrigatórios");
    }

    @Test
    void rejectsMalformedInnerJson() throws Exception {
        respondWith("isto não é um JSON válido");

        assertThatThrownBy(() -> provider(Duration.ofSeconds(5)).analyze(context()))
                .isInstanceOf(AgentProviderException.class)
                .hasMessageContaining("schema esperado");
    }

    @Test
    void rejectsInvalidConfidenceValue() throws Exception {
        String inner = objectMapper.writeValueAsString(Map.of(
                "summary", "Setup válido.",
                "evidence", List.of("evidência"),
                "confidence", "MUITO_ALTA",
                "invalidations", List.of()));
        respondWith(inner);

        assertThatThrownBy(() -> provider(Duration.ofSeconds(5)).analyze(context()))
                .isInstanceOf(AgentProviderException.class)
                .hasMessageContaining("fora do schema");
    }

    @Test
    void rejectsResponseViolatingLanguageGuard() throws Exception {
        String inner = objectMapper.writeValueAsString(Map.of(
                "summary", "Compre agora antes que suba mais!",
                "evidence", List.of("momentum positivo"),
                "confidence", "HIGH",
                "invalidations", List.of()));
        respondWith(inner);

        assertThatThrownBy(() -> provider(Duration.ofSeconds(5)).analyze(context()))
                .isInstanceOf(AgentProviderException.class)
                .hasMessageContaining("regra de linguagem");
    }

    @Test
    void timesOutWhenOllamaIsTooSlow() throws IOException {
        AtomicInteger requestsReceived = new AtomicInteger();
        server.createContext("/api/generate", exchange -> {
            requestsReceived.incrementAndGet();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> provider(Duration.ofMillis(100)).analyze(context()))
                .isInstanceOf(AgentProviderException.class)
                .matches(e -> ((AgentProviderException) e).isTimeout());
        assertThat(requestsReceived.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void mapsNonOkStatusToNonTimeoutException() throws IOException {
        server.createContext("/api/generate", exchange -> {
            byte[] bytes = "internal error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> provider(Duration.ofSeconds(5)).analyze(context()))
                .isInstanceOf(AgentProviderException.class)
                .matches(e -> !((AgentProviderException) e).isTimeout())
                .hasMessageContaining("HTTP 500");
    }
}
