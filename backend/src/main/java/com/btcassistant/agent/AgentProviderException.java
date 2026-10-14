package com.btcassistant.agent;

/** Falha ao chamar o provedor de IA — nunca deve propagar além do {@code AgentOrchestrator} (SDD §34/§35). */
public class AgentProviderException extends Exception {

    private final boolean timeout;

    public AgentProviderException(String message, boolean timeout, Throwable cause) {
        super(message, cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
