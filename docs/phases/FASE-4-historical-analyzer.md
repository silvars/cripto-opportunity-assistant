# Fase 4 — Historical Analyzer — SDD

**Depende de:** Fase 3 (Opportunity + Risk)
**Módulos:** `historical`
**Referência:** [SDD.md](../../SDD.md) seções 27–31, 59–61, 67

---

## 1. Objetivo

Comparar o evento/contexto atual com situações históricas semelhantes, calculando resultados futuros (retorno, MFE/MAE, tempo até alvo/stop) de forma **determinística e livre de lookahead**. Esta é a fase mais sensível do sistema — tem teste automatizado obrigatório.

## 2. Escopo

**Dentro:**

- Historical Feature Store: features normalizadas pré-calculadas e persistidas para consulta rápida
- Cálculo de similaridade (features normalizadas + distância ponderada, configurável e versionada)
- Resultados históricos por horizonte (5m, 15m, 30m, 1h, 2h, 4h, 12h, 24h): `futureReturn`, `MFE`, `MAE`, `timeToTarget`, `timeToStop`, `targetBeforeStop`
- Integração com o `OpportunityEngine` da Fase 3 (preenche `historicalSupport`, antes `UNKNOWN`)
- Backtest cronológico reprodutível
- **Teste automatizado de lookahead** (obrigatório, bloqueante para merge)

**Fora:**

- Vector DB / embeddings (decisão explícita de não usar — seção 3 do SDD)
- Agent/Ollama

## 3. Modelo de dados

```sql
-- V6__create_historical_feature_store.sql
CREATE TABLE historical_feature_store (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    event_id UUID NOT NULL REFERENCES market_events(event_id),
    event_type VARCHAR(40) NOT NULL,
    normalized_features JSONB NOT NULL,
    historical_version VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_hfs_symbol_type ON historical_feature_store (symbol, event_type);

-- V7__create_historical_outcomes.sql
CREATE TABLE historical_outcomes (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES market_events(event_id),
    horizon VARCHAR(10) NOT NULL, -- '5m','15m',...,'24h'
    future_return NUMERIC(10,6),
    mfe NUMERIC(10,6),
    mae NUMERIC(10,6),
    time_to_target INTERVAL,
    time_to_stop INTERVAL,
    target_before_stop BOOLEAN,
    evaluation_end TIMESTAMPTZ NOT NULL, -- event_time + horizon: quando o outcome ficou completamente conhecido
    target_definition_version VARCHAR(10) NOT NULL,
    stop_definition_version VARCHAR(10) NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_outcome UNIQUE (event_id, horizon)
);
```

`historical_outcomes` só pode ser preenchido com dados que já existiam no momento do cálculo — nunca antes do horizonte ter efetivamente decorrido no tempo real/backtest. `evaluation_end` é o campo que distingue "quando o evento aconteceu" de "quando seu resultado ficou totalmente conhecido" (SDD §30/31).

## 4. Contratos / Interfaces

```java
public interface HistoricalAnalyzer {
    HistoricalContext findSimilar(DetectedEvent event, Instant asOf);
}

public record HistoricalContext(
    int sampleSize,
    List<SimilarEvent> similarEvents,
    Map<String, OutcomeStats> outcomesByHorizon,
    String historicalVersion
) {}

public record OutcomeStats(
    BigDecimal targetBeforeStopProbability,
    Duration medianTimeToTarget,
    BigDecimal medianAdverseMove
) {}
```

**Regra crítica (SDD §31):** `findSimilar(event, asOf)` só pode considerar um evento candidato se:

```text
candidate.eventTime < asOf                     (estrito, nunca <=)
candidate.eventId != event.eventId             (nunca compara o evento consigo mesmo)
```

e, para cada outcome agregado:

```text
candidate.outcome.evaluationEnd <= asOf
```

Não basta o evento ter ocorrido antes de `asOf` — o outcome usado também precisa estar completamente conhecido em `asOf`. Qualquer violação é bug crítico.

## 5. Configuração

```yaml
historical-analyzer:
  version: "1.0"
  similarity:
    weights:
      magnitude: 0.3
      duration: 0.15
      volume-ratio: 0.2
      volatility: 0.2
      momentum: 0.15
    min-sample-size: 30

research:
  outcome:
    target-definition-version: "1.0" # ex.: target = +1%, definido/versionado antes do EXP-001
    stop-definition-version: "1.0"   # ex.: stop = -1%
```

## 6. Fluxo

```text
event_detected
      ↓
HistoricalAnalyzer.findSimilar(event, asOf = event.eventTime)
      ↓
busca em historical_feature_store (candidate.eventTime < asOf, candidate.eventId != event.eventId)
      ↓
calcula distância ponderada → N eventos mais similares
      ↓
agrega historical_outcomes desses eventos (apenas outcome.evaluationEnd <= asOf)
      ↓
HistoricalContext → alimenta OpportunityEngine.historicalSupport
```

## 7. Critérios de aceite (DoD)

- `sampleSize` sempre exibido junto de qualquer estatística histórica
- Amostra abaixo de `min-sample-size` → `historicalContext = UNKNOWN`, nunca número "forçado"
- Backtest executado duas vezes com mesmo dataset/config produz resultado idêntico
- **Teste de lookahead passa**: resultado de uma decisão em `T` não muda ao adicionar candles `T+1, T+2, ...`
- Nenhum candidato com `eventTime >= asOf` ou `eventId == event.eventId` é retornado; nenhum outcome com `evaluationEnd > asOf` é agregado
- Pesos de similaridade configuráveis e versionados (`historicalVersion`); metodologia de target/stop versionada (`targetDefinitionVersion`/`stopDefinitionVersion`) separadamente

## 8. Testes obrigatórios

- **Teste de lookahead (crítico/bloqueante):** executar `findSimilar` em `T`, inserir dados futuros no banco de teste, executar novamente em `T` → asserts de igualdade byte-a-byte do resultado
- **Teste de fronteira:** evento candidato com `eventTime == asOf` deve ser rejeitado (estrito); outcome com `evaluationEnd == asOf` deve ser aceito; `evaluationEnd == asOf + 1ms` deve ser rejeitado
- **Unit:** cálculo de similaridade com features sintéticas conhecidas
- **Integration:** backtest cronológico sobre um subconjunto de dados reais, validando reprodutibilidade
- **Performance:** medir tempo de busca de similaridade sobre 12 meses de histórico (525k candles/ano); se lento, considerar índice adicional antes de otimizações maiores

## 9. Observabilidade

Eventos: `historical_context_computed`, `historical_context_unavailable` (amostra insuficiente).

## 10. Riscos conhecidos

- Buscar por similaridade "na unha" (sem vector DB) pode degradar com o crescimento do histórico multi-ano — se isso acontecer, considerar pré-filtragem por `event_type`/faixa de magnitude antes de calcular distância completa, mantendo a decisão de não usar vector DB/embeddings.
