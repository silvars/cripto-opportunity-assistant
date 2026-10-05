# Fase 2 — Event Detector — SDD

**Depende de:** Fase 1 (Feature Engine)
**Módulos:** `event`
**Referência:** [SDD.md](../../SDD.md) seções 20, 21

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
