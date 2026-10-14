package com.btcassistant.agent;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sempre reporta UP — Ollama indisponível nunca derruba o health geral do sistema (SDD §74/§75).
 * O detalhe {@code ollamaReachable} comunica a disponibilidade real sem afetar o status agregado.
 */
@Component("ollama")
public class OllamaHealthIndicator implements HealthIndicator {

    private static final Duration PING_TIMEOUT = Duration.ofSeconds(2);

    private final AgentProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public OllamaHealthIndicator(AgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up().withDetail("ollamaReachable", false).withDetail("reason", "agent disabled").build();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl() + "/api/tags"))
                    .timeout(PING_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return Health.up().withDetail("ollamaReachable", response.statusCode() == 200).build();
        } catch (java.io.IOException e) {
            return Health.up().withDetail("ollamaReachable", false).withDetail("error", e.getMessage()).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Health.up().withDetail("ollamaReachable", false).withDetail("error", "interrupted").build();
        }
    }
}
