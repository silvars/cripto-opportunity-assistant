# Fase 2 — Event Detector — SDD

**Depende de:** Fase 1 (Feature Engine)
**Módulos:** `event`
**Referência:** [SDD.md](../SDD.md) seções 20, 21

---

## 1. Objetivo

Transformar features em eventos semânticos (ocorrências quantitativas), imutáveis e versionados.

## 2. Escopo

**Dentro:**

- Detecção dos 12 eventos: `RAPID_DROP`, `RAPID_RISE`, `VOLUME_SPIKE`, `VOLATILITY_EXPANSION`, `VOLATILITY_CONTRACTION`, `LOCAL_LOW_APPROACH`, `LOCAL_HIGH_APPROACH`, `RECOVERY`, `FAILED_RECOVERY`, `FAILED_BREAKOUT`, `BREAKOUT`, `BREAKDOWN`
- Persistência imutável de eventos
- Versionamento (`engineVersion`)
- Exposição via API (`GET /api/v1/events`)

**Fora:**

- Opportunity/Risk Engine, Historical Analyzer, Agent

## 3. Modelo de dados

```sql
-- V3__create_market_events.sql
CREATE TABLE market_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(20) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    price NUMERIC(20,8) NOT NULL,
    features_snapshot JSONB NOT NULL,
    magnitude NUMERIC(10,6),
    duration INTERVAL,
    engine_version VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_symbol_time ON market_events (symbol, event_time DESC);
CREATE INDEX idx_events_type ON market_events (event_type);
```

Não há `UPDATE` sobre `market_events` em código de aplicação — apenas `INSERT`. Mudança de algoritmo gera novos eventos com `engine_version` incrementado; eventos antigos permanecem.

## 4. Contratos / Interfaces

```java
public interface EventRule {
    String eventType();
    String engineVersion();
    Optional<DetectedEvent> evaluate(FeatureSnapshot features, FeatureHistory recentHistory);
}

public record DetectedEvent(
    String symbol,
    String eventType,
    Instant eventTime,
    BigDecimal price,
    Map<String, BigDecimal> featuresSnapshot,
    BigDecimal magnitude,
    Duration duration,
    String engineVersion
) {}
```

Cada `EventRule` é isolada e configurável (uma classe por tipo de evento); um `EventDetector` orquestrador roda todas as regras a cada novo `FeatureSnapshot`.

## 5. Configuração

```yaml
event-detector:
  engine-version: "1.0"
  rules:
    rapid-drop:
      window: 15m
      threshold: -0.02
    volume-spike:
      zscore: 2.5
    volatility-expansion:
      ratio: 1.5
```

Thresholds experimentais — não tratar como verdade estatística (seção 28 do SDD).

## 6. Fluxo

```text
feature_update (evento interno)
      ↓
EventDetector.evaluate(todas as EventRule)
      ↓
DetectedEvent (0..n)
      ↓
MarketEventRepository.insert() (imutável)
      ↓
event_detected (evento interno/WebSocket)
```

## 7. Critérios de aceite (DoD)

- Os 12 tipos de evento implementados e configuráveis via YAML
- Eventos nunca são atualizados após criados (teste garante isso)
- `engine_version` registrado em cada evento
- Mudar um threshold de config não altera eventos já persistidos
- API lista eventos recentes por símbolo

## 8. Testes obrigatórios

- **Unit:** cada `EventRule` testada com sequência sintética de features (caso positivo e negativo)
- **Imutabilidade:** tentativa de alterar evento existente deve ser impossível pelo código de aplicação (sem método de update exposto)
- **Determinismo:** mesma sequência de features + mesma config → mesmos eventos
- **No-lookahead:** evento detectado em `T` não pode depender de feature calculada com candle `> T`

## 9. Observabilidade

Evento de log: `event_detected` (já previsto na seção 73 do SDD principal).

## 10. Riscos conhecidos

- Eventos podem ser gerados em alta frequência durante movimentos prolongados — deduplicação/cooldown fica a cargo do `alert` (Fase 3), não do `event` em si. O Event Detector registra todos os eventos brutos; o agrupamento para o usuário acontece depois.

## 11. Decisões de implementação (onde a especificação original era ambígua)

O SDD lista os 12 tipos de evento (seção 20) e o modelo de dados (seção 21), mas não define as fórmulas de detecção nem todos os detalhes de schema. Decisões tomadas durante a implementação:

- **Assinatura de `EventRule`**: ajustada para `evaluate(BigDecimal price, FeatureSnapshot features, FeatureHistory recentHistory)` — o rascunho original omitia `price`, mas `DetectedEvent` o exige e `FeatureSnapshot` (Fase 1) não carrega preço. `EventDetector` busca o preço via `CandleRepository` pelo mesmo `(symbol, interval, candleOpenTime)` do snapshot.
- **Gatilho**: implementado como `FeatureCalculatedEvent` (Spring `ApplicationEvent`, análogo ao `CandlePersistedEvent` da Fase 1), publicado por `FeatureCalculationListener` **apenas quando o snapshot é novo** (`FeaturePersister.persist(...) == true`) — evita disparar detecção de eventos para snapshots já existentes.
- **`FeatureHistory`**: os 5 snapshots de features mais recentes **estritamente anteriores** ao candle atual (nunca `>=`, preservando no-lookahead). Usada apenas pelas regras que precisam comparar o estado atual com o imediatamente anterior (`BREAKOUT`, `BREAKDOWN`, `RECOVERY`, `FAILED_RECOVERY`, `FAILED_BREAKOUT`).
- **Fórmulas de detecção** (nenhuma definida explicitamente no SDD — apenas os nomes dos eventos):
  - `RAPID_DROP`/`RAPID_RISE`: `return_15m` cruza o threshold configurado.
  - `VOLUME_SPIKE`: `volume_zscore >= threshold`.
  - `VOLATILITY_EXPANSION`/`VOLATILITY_CONTRACTION`: razão `volatility_15m / volatility_4h` acima/abaixo do threshold.
  - `LOCAL_LOW_APPROACH`/`LOCAL_HIGH_APPROACH`: `distance_recent_low_1h`/`distance_recent_high_1h` dentro de uma margem pequena de zero.
  - `BREAKOUT`/`BREAKDOWN`: usa `FeatureHistory` — dispara apenas na **transição** (snapshot anterior fora da máxima/mínima de 24h, snapshot atual exatamente nela), não em todo candle em que o preço já estava na máxima/mínima.
  - `RECOVERY`: snapshot anterior próximo da mínima de 1h + snapshot atual com retorno de 15m acima do threshold de recuperação.
  - `FAILED_RECOVERY`: snapshot anterior com momentum de 15m positivo (recuperando) + snapshot atual reverte para retorno negativo e continua perto da mínima.
  - `FAILED_BREAKOUT`: snapshot anterior tinha acabado de romper a máxima de 24h + snapshot atual já recuou abaixo dela com retorno negativo.
- **`EventProperties`**: estrutura simplificada para um único objeto `Rules` plano (`event-detector.rules.rapid-drop-threshold`, etc.) em vez de um objeto aninhado por regra do rascunho original (`rules.rapid-drop.window`/`.threshold`) — evita 10 classes de configuração quase idênticas para um conjunto de thresholds simples.
- **Coluna `duration`**: a tabela usa `duration_seconds BIGINT` em vez do tipo nativo `INTERVAL` do rascunho original — evita a complexidade de mapear `INTERVAL` do Postgres via Hibernate/JPA; a informação (duração em segundos) é equivalente e suficiente para esta fase.
- **`features_snapshot` (JSONB)**: mapeado via `@JdbcTypeCode(SqlTypes.JSON)` do Hibernate (suporte nativo a `Map<String,BigDecimal>` ↔ `jsonb`, validado contra PostgreSQL real via Testcontainers) — não foi necessário nenhum conversor/dependência extra.
- **`event_id`**: gerado em código (`UUID.randomUUID()`) na construção da entidade, não via `DEFAULT gen_random_uuid()` do banco — mais previsível e testável, sem depender de uma função específica do Postgres estar disponível.
- **Publicação WebSocket de eventos** (`event_detected` no fluxo) foi **adiada**, mesma decisão da Fase 1 para features — não exigida pelo DoD desta fase (que só pede a API REST `GET /api/v1/events/{symbol}`).
