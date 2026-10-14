package com.btcassistant.agent;

/** Abstração do provedor de IA — o domínio não conhece diretamente o Ollama (SDD §33). */
public interface AgentProvider {

    /** @throws AgentProviderException se a chamada falhar ou expirar — nunca lança para erros de negócio normais. */
    AgentNarrative analyze(AgentContext context) throws AgentProviderException;
}
