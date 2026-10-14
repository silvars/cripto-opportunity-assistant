# Fase 4 — Historical Analyzer — SDD

**Depende de:** Fase 3 (Opportunity + Risk)
**Módulos:** `historical`
**Referência:** [SDD.md](../SDD.md) seções 27–31, 59–61, 67

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

## 11. Decisões de implementação (onde a especificação original era ambígua)

- **Exclusão do próprio evento (SDD §31) garantida apenas pelo filtro de tempo**: o rascunho original lista `candidate.eventId != event.eventId` como uma regra separada de `candidate.eventTime < asOf`. Como `asOf` é sempre o `eventTime` do evento avaliado, nenhuma linha com esse mesmo `eventTime` (incluindo a do próprio evento) jamais passa no filtro estrito `<` — a exclusão por id seria redundante. A query (`HistoricalFeatureStoreRepository.findCandidates`) não recebe nem precisa de um parâmetro de exclusão por id.
- **`HistoricalContext` migrou de módulo**: o placeholder vazio `opportunity.HistoricalContext` da Fase 3 foi removido; o record real (com `sampleSize`/`similarEvents`/`outcomesByHorizon`/`historicalVersion`) agora vive em `com.btcassistant.historical`, já que é esse módulo que o produz. `opportunity` passou a depender de `historical` para esse tipo (direção correta — dados fluem de `historical` para `opportunity`, nunca o contrário).
- **`ScoreComponentCalculator.calculate(...)` ganhou um parâmetro `Optional<HistoricalContext>`**: necessário para `HistoricalSupportComponent` acessar o contexto; os outros 5 componentes simplesmente ignoram o parâmetro. Mantém o design uniforme de "lista de calculadores" do `OpportunityEngineImpl` em vez de tratar `historicalSupport` como um caso especial fora do loop.
- **`HistoricalSupportComponent` usa `targetBeforeStopProbability` do horizonte canônico** (`historical-analyzer.canonical-horizon`, default `1h`) diretamente como valor normalizado — já nasce em [0,1], sem transformação adicional. Se o horizonte canônico não tiver dados suficientes nesta avaliação específica (mas outros horizontes tiverem), cai para a média dos horizontes disponíveis, em vez de ficar UNKNOWN desnecessariamente.
- **Normalização de features não precisa ser "por família de evento"** como em `opportunity.EventStrengthComponent` (Fase 3): como a busca de candidatos sempre filtra por `event_type` igual, os valores já nascem na mesma unidade dentro de um mesmo tipo — não há comparação cross-type a normalizar. A única correção necessária foi `momentum`: a feature `momentum_15m` é um delta de preço absoluto (decisão da Fase 1), o que a tornaria enviesada por nível de preço ao comparar eventos de meses diferentes (BTC a 20k vs. 100k) — corrigido dividindo pelo preço do próprio evento.
- **`distanceToExtreme` é computado e armazenado, mas não participa do cálculo de distância**: a seção 5 do rascunho original lista apenas 5 pesos (magnitude, duration, volume-ratio, volatility, momentum) para as 7 "features" citadas na seção 27 (que inclui `eventType` — usado como filtro rígido, não dimensão de distância — e `distanceToExtreme`, sem peso definido). Em vez de inventar um 6º peso não especificado, a dimensão é mantida fora do cálculo, disponível para uso futuro (dashboard/debug).
- **Dimensões ausentes em qualquer lado do par são excluídas E o peso é renormalizado** sobre as dimensões efetivamente disponíveis para aquele par específico — mesmo princípio de renormalização já usado em `opportunity` (Fase 3) e features UNKNOWN (Fase 1). Nunca trata ausência como zero.
- **`sampleSize` = tamanho da lista de candidatos efetivamente selecionada** (após ordenar por similaridade e aplicar o corte `max-similar-events`, default 50), não o total bruto de candidatos elegíveis por tempo. Essa é a leitura mais literal do DoD ("amostra abaixo de `min-sample-size`") — representa quantos eventos similares de fato embasam a comparação, não quantos existem no banco.
- **`outcomesByHorizon` omite horizontes sem dados suficientes** (mesmo princípio "nunca fabricar número" das fases anteriores): um horizonte só aparece no mapa se houver pelo menos 1 outcome com `targetBeforeStop` resolvido (não nulo) E pelo menos 1 com `timeToTarget` preenchido E pelo menos 1 com `mae` preenchido — caso contrário, omitido (nunca com um valor `0`/neutro implícito).
- **Colunas `time_to_target`/`time_to_stop`** usam `BIGINT` (segundos) em vez do tipo `INTERVAL` do rascunho original — mesma decisão e mesma justificativa da Fase 2 para `duration` em `market_events` (evita a complexidade de mapear `INTERVAL` via Hibernate/JPA).
- **Empate entre target e stop no mesmo candle (granularidade de 1m)**: assume-se que o stop ocorreu primeiro (decisão conservadora de risco) — não há como saber a ordem exata intra-candle sem dados de tick.
- **`HistoricalOutcomeBackfillService` faz varredura completa de `market_events`** a cada execução (`@Scheduled`, default a cada 5 minutos) em vez de rastrear incrementalmente o progresso — aceitável nesta fase (mesma postura do SDD §10 para a busca de similaridade: só otimizar quando o volume realmente demandar).
- **`research.outcome.target-percentage`/`stop-percentage`** (ambos default `0.01` = 1%) foram adicionados à configuração — o rascunho original versiona a *metodologia* (`target-definition-version`) mas não dá os valores numéricos reais, apenas um comentário de exemplo ("ex.: target = +1%"). Os valores viraram configuração explícita, versionada junto.
