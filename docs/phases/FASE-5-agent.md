# Fase 5 — Agent (IA) — SDD

**Depende de:** Fase 4 (Historical Analyzer)
**Módulos:** `agent`
**Referência:** [SDD.md](../SDD.md) seções 32–40, 74–75

---

## 1. Objetivo

Adicionar interpretação por IA local (Ollama) sobre as evidências já calculadas deterministicamente — sem que o LLM calcule nenhuma métrica, sem que o sistema dependa dele para funcionar.

## 2. Escopo

**Dentro:**

- `AgentProvider` (interface) + `OllamaAgentProvider` (implementação; avaliar Spring AI `ChatClient`)
- Agent Orchestrator: decide quando chamar (score ≥ `watch-threshold`), monta contexto, timeout, retry, fallback
- Market Analyst: agente único com tools (`getCurrentMarketState`, `getMarketFeatures`, `getRecentEvents`, `searchSimilarHistoricalEvents`, `getOpportunityContext`, `getRiskContext`, `getRecentAlerts`), sempre operando sobre o instrumento recebido via `InstrumentContext` — nunca assumindo Bitcoin no prompt ou no código
- Saída estruturada em JSON, validada por schema
- Graceful degradation: se Ollama cair, pipeline quantitativo continua funcionando normalmente

**Fora:**

- Múltiplos agentes independentes
- Fontes externas (notícias, macro, on-chain) — tools retornam apenas dados já existentes no sistema

## 3. Modelo de dados

```sql
-- V8__create_agent_analyses.sql
CREATE TABLE agent_analyses (
    analysis_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id UUID NOT NULL REFERENCES alerts(alert_id),
    analysis_version INT NOT NULL DEFAULT 1,
    classification VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    score NUMERIC(5,2) NOT NULL,
    confidence VARCHAR(10) NOT NULL,
    summary TEXT NOT NULL,
    evidence JSONB NOT NULL,
    historical_context JSONB,
    risk JSONB NOT NULL,
    invalidations JSONB,
    agent_version VARCHAR(10) NOT NULL,
    prompt_version VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL, -- COMPLETED / FAILED / TIMEOUT
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_alert_analysis_version UNIQUE (alert_id, analysis_version)
);
```

Se houver reanálise, cria-se `analysis_version` incremental — a análise original nunca é sobrescrita (seção 34 do SDD anterior).

## 4. Contratos / Interfaces

```java
public interface AgentProvider {
    AgentAnalysis analyze(AgentContext context);
}

public record InstrumentContext(
    String symbol,
    String baseAsset,
    String quoteAsset,
    String exchange,
    String interval
) {}

public record AgentContext(
    InstrumentContext instrument,
    Map<String, Object> market,
    Map<String, Object> features,
    DetectedEvent event,
    OpportunityResult opportunity,
    RiskAssessment risk,
    HistoricalContext historicalContext,
    List<AlertSummary> recentAlerts
) {}

public record AgentAnalysis(
    Classification classification,
    Direction direction,
    BigDecimal score,
    Confidence confidence,
    String summary,
    List<String> evidence,
    HistoricalContextSummary historicalContext,
    RiskSummary risk,
    List<String> invalidations,
    String agentVersion,
    String promptVersion,
    AnalysisStatus status
) {}

public enum AnalysisStatus { COMPLETED, FAILED, TIMEOUT }
```

Campos sem dado disponível devem ser serializados como `null` com `"status": "not_available"` — nunca inventados pelo agente (seção 4.2 do SDD).

## 5. Configuração

```yaml
agent:
  enabled: true
  timeout: 30s
  max-retries: 1
  trigger-threshold: 50 # dispara a partir de WATCH
  provider: ollama
  ollama:
    base-url: http://host.docker.internal:11434
    model: ${OLLAMA_MODEL}
  version: "1.0"
  prompt-version: "1.0"
```

## 6. Fluxo

```text
opportunity_created (score >= trigger-threshold)
      ↓
AgentOrchestrator monta AgentContext (market, features, event, opportunity, risk, historicalContext, recentAlerts)
      ↓
AgentProvider.analyze()  [timeout 30s, 1 retry]
      ↓
sucesso → valida schema JSON → persiste AgentAnalysis (COMPLETED)
falha/timeout → persiste AgentAnalysis (FAILED/TIMEOUT) → Alert segue sem análise de IA
      ↓
agent_analysis_completed / agent_analysis_failed (WebSocket)
```

## 7. Critérios de aceite (DoD)

- Ollama fora do ar não impede geração de alertas nem derruba o `/health` geral (seção 75 do SDD)
- Saída do agente sempre valida contra o schema esperado antes de ser aceita; resposta inválida é descartada e logada como falha, nunca exposta ao usuário como se fosse válida
- Agente nunca retorna métrica numérica que não veio do `AgentContext`
- Linguagem do agente nunca usa "compre agora" (teste de regressão sobre templates/prompt)
- `agentVersion`/`promptVersion` registrados em cada análise

## 8. Testes obrigatórios

- **Contract test:** resposta do provider validada contra JSON schema
- **Timeout/retry:** simular Ollama lento/indisponível, validar fallback sem bloquear pipeline
- **Graceful degradation:** com Ollama desligado, dashboard e alertas quantitativos continuam funcionando
- **Regra anti-invenção:** contexto com campo ausente → resposta deve conter `not_available`, nunca um valor plausível inventado

## 9. Observabilidade

Eventos: `agent_started`, `agent_completed`, `agent_failed` (já previstos na seção 73 do SDD principal).

## 10. Riscos conhecidos

- Modelos locais variam muito em aderência a JSON estrito — reforçar com retry + validação de schema, não com confiança no prompt isoladamente.

## 11. Decisões de implementação

Desvios e decisões tomadas durante a implementação real, em relação ao desenho inicial acima:

- **Sem arquitetura de "tools" chamadas pelo LLM.** O desenho original previa um agente que chama
  tools (`getCurrentMarketState`, `getMarketFeatures`, etc.) sob demanda. A implementação real
  busca TODO o contexto adiantado (`AgentContextBuilder`) e inclui tudo em um único prompt
  (`PromptBuilder`), numa única chamada HTTP ao Ollama (`/api/generate`, `stream: false,
  format: "json"`). Isso é mais simples, determinístico e fácil de testar/auditar do que um loop
  de tool-calling, e `llama3.1:8b` rodando localmente em hardware modesto (Mac i7/16GB) não se
  beneficiaria de latência extra de múltiplas idas e vindas. Caso o modelo evolua para suportar
  tool-calling nativo com mais confiabilidade, essa decisão pode ser revisitada.
- **`classification`/`direction`/`score`/`risk`/`historicalContext` são sempre ecoados do que já
  foi calculado deterministicamente** (`MarketOpportunityEntity`, `RiskAssessment`,
  `HistoricalContext` já persistidos) — o LLM nunca os decide, apenas interpreta. Apenas
  `summary`/`evidence`/`confidence`/`invalidations` vêm da resposta do modelo
  (`AgentNarrative`/`AgentNarrativeJson`). Isso é reforçado estruturalmente: o tipo de retorno de
  `AgentProvider.analyze(...)` é `AgentNarrative` (só os 4 campos interpretativos), nunca
  `AgentAnalysis` completo — o orquestrador (`AgentOrchestrator`) é quem monta o `AgentAnalysis`
  final combinando o que já existia com a narrativa do LLM.
- **Reuso de tipos existentes em vez de novos tipos espelhados.** `AgentContext.opportunity` usa
  `MarketOpportunityEntity` (já persistida) em vez de recriar um `OpportunityResult` novo;
  `AgentContext.risk`/`AgentAnalysis.risk` usam `risk.RiskAssessment` em vez de um `RiskSummary`
  dedicado — evita duplicar a mesma informação em dois formatos.
- **Trigger no evento certo.** O agente reage a `AlertCreatedEvent` (publicado pelo `AlertService`
  apenas quando um alerta NOVO é de fato criado, nunca em ocorrências deduplicadas — ver Fase 3),
  não a um evento genérico de "oportunidade criada". Isso evita rodar o LLM repetidamente para o
  mesmo movimento de mercado já alertado.
- **`OllamaHealthIndicator` sempre retorna `Health.up()`**, nunca `down()`, mesmo com Ollama
  inacessível — o status real vai no detail `ollamaReachable` (SDD §75: "Ollama indisponível não
  derruba health geral"). Deliberadamente não usamos uma modelagem alternativa (ex.: status
  `UNKNOWN`) para manter o agregado de `/actuator/health` simples de consumir por quem só olha
  `status == UP/DOWN`.
- **Spring Boot 4 moveu toda a infraestrutura de health check para um módulo novo**,
  `org.springframework.boot:spring-boot-health` (vem transitivamente via
  `spring-boot-starter-actuator`, sem necessidade de dependência explícita no `pom.xml`). As
  classes `Health`/`HealthIndicator` saíram de `org.springframework.boot.actuate.health` e agora
  vivem em `org.springframework.boot.health.contributor` — mesma API (`Health.up().withDetail(...)
  .build()`), só o pacote mudou.
- **Retry simples sem backoff.** `max-retries` (config) tenta novamente na mesma chamada síncrona,
  sem espera entre tentativas — adequado porque o próprio timeout de 30s por tentativa já limita o
  tempo total, e o volume de chamadas (só em alertas novos acima do threshold) é baixo o suficiente
  para não sobrecarregar um Ollama local rodando na mesma máquina.
- **`docker-compose.yml`**: como o Ollama roda no HOST (instalado pelo usuário fora do stack
  Docker, não como um container do `docker-compose.yml`), `OLLAMA_BASE_URL` dentro do container
  `api` aponta para `http://host.docker.internal:11434` (não `http://localhost:11434`, que dentro
  de um container resolveria para o próprio container). Adicionado `extra_hosts:
  host.docker.internal:host-gateway` para portabilidade em Linux (no Docker Desktop macOS/Windows
  isso já funciona nativamente).
