package com.btcassistant.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/** Config-driven — nunca hardcoded (SDD §34/§5). */
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private boolean enabled = true;
    private Duration timeout = Duration.ofSeconds(30);
    private int maxRetries = 1;
    private BigDecimal triggerThreshold = new BigDecimal("50");
    private String version = "1.0";
    private String promptVersion = "1.0";
    private Ollama ollama = new Ollama();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public BigDecimal getTriggerThreshold() {
        return triggerThreshold;
    }

    public void setTriggerThreshold(BigDecimal triggerThreshold) {
        this.triggerThreshold = triggerThreshold;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public void setOllama(Ollama ollama) {
        this.ollama = ollama;
    }

    public static class Ollama {

        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.1:8b";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
