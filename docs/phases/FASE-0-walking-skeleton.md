# Fase 0 — Walking Skeleton — SDD

**Depende de:** nada (primeira fase)
**Módulos:** `market`, `shared`, `configuration`
**Referência:** [SDD.md](../../SDD.md) seções 5–17, 73–87

---

## 1. Objetivo

Provar que toda a infraestrutura funciona ponta a ponta — Binance → Spring Boot → PostgreSQL → Next.js — sem nenhuma lógica de negócio. Nenhum algoritmo de trading, feature, evento, oportunidade, risco, histórico ou IA é implementado nesta fase.

## 2. Escopo

**Dentro:**

- Monorepo: `backend/` (Maven) + `frontend/` (pnpm/Next.js)
- Docker Compose: `postgres`, `api`, `web`
- Flyway migrations
- Health checks (Spring Actuator)
- Módulo `market`:
  - interface `MarketDataProvider` + `CandleListener` (seção 12 do SDD)
  - implementação `BinanceMarketDataProvider` (WebSocket live + REST backfill)
  - validação de candle (seção 17)
  - detecção e recuperação de gaps (seções 14, 19, 20)
  - reconexão com backoff exponencial (seção 13)
  - persistência de candle (`UNIQUE(symbol, interval, open_time)`)
- Configuração de instrumento via `market.instruments` (apenas BTCUSDT habilitado)
- API REST mínima (`GET /health`, `GET /api/v1/market/candles`) + WebSocket para o frontend
- Dashboard Next.js: preço atual + atualização em tempo real (sem gráfico avançado, sem oportunidade/evento)
- Logs estruturados (seção 73)

**Fora (explicitamente adiado):**

- Feature Engine, Event Detector, Opportunity Engine, Risk Engine, Historical Analyzer, Agent/Ollama
- Qualquer tela além de preço/candle ao vivo

## 3. Modelo de dados

```sql
-- V1__create_candles.sql
CREATE TABLE candles (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    interval VARCHAR(10) NOT NULL,
    open_time TIMESTAMPTZ NOT NULL,
    close_time TIMESTAMPTZ NOT NULL,
    open NUMERIC(20,8) NOT NULL,
    high NUMERIC(20,8) NOT NULL,
    low NUMERIC(20,8) NOT NULL,
    close NUMERIC(20,8) NOT NULL,
    volume NUMERIC(24,8) NOT NULL,
    quote_volume NUMERIC(24,8) NOT NULL,
    trade_count INTEGER NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_candle UNIQUE (symbol, interval, open_time)
);

CREATE INDEX idx_candles_symbol_interval_time ON candles (symbol, interval, open_time DESC);
```

Entidade JPA usa `BigDecimal` para preço/volume e `Instant` para timestamps (seção 11 do SDD). Nunca `double`.

## 4. Contratos / Interfaces

```java
public interface MarketDataProvider {
    void streamLive(Instrument instrument, CandleListener listener);
    List<Candle> fetchHistorical(Instrument instrument, Instant from, Instant to);
}

public interface CandleListener {
    void onCandle(Candle candle);
}

public record Instrument(
    String symbol,
    String baseAsset,
    String quoteAsset,
    String exchange,
    String interval,
    boolean enabled
) {}
```

`BinanceMarketDataProvider` implementa ambos os métodos usando client WebSocket (thread dedicada) e client REST. Nenhum tipo reativo (Reactor/WebFlux) é utilizado — callback simples entregue em thread própria (ADR-009).

## 5. Configuração

```yaml
market:
  instruments:
    - symbol: BTCUSDT
      interval: 1m
      enabled: true
      historical-backfill:
        enabled: true
        period: 12-months

websocket:
  reconnect:
    initial-delay: 1s
    max-delay: 30s
    multiplier: 2
```

## 6. Fluxo

```text
Binance WS/REST
      ↓
BinanceMarketDataProvider
      ↓
CandleListener.onCandle()
      ↓
Validator (seção 17)
      ↓
CandleRepository (JPA, bloqueante)
      ↓
Publish to cockpit WebSocket sessions
      ↓
Next.js dashboard atualiza
```

## 7. Critérios de aceite (Definition of Done)

- `docker compose up` sobe `postgres`, `api`, `web` sem erro
- Flyway aplica `V1__create_candles.sql` automaticamente
- `BinanceMarketDataProvider` conecta ao WebSocket público da Binance e recebe candles BTCUSDT 1m
- Candle é validado antes de persistir; candle inválido gera log `candle_rejected` e não entra no banco
- Duplicata (mesma `symbol+interval+open_time`) é ignorada, não gera erro
- Backfill REST de 12 meses roda no primeiro boot, em chunks, sem duplicar dados ao rodar duas vezes
- Gap é detectado e recuperado automaticamente via REST
- Desconexão força reconexão com backoff exponencial (logs `websocket_disconnected`/`websocket_reconnecting`/`websocket_connected`)
- `GET /health` reporta estado de `API`, `DATABASE`, `BINANCE` separadamente
- Frontend mostra preço atual e atualiza sem refresh manual
- API continua respondendo mesmo com frontend fechado

## 8. Testes obrigatórios

- **Unit:** validação de candle (preço negativo, high < low, timestamp fora de ordem)
- **Integration (Testcontainers):** persistência com constraint única, migration aplicada, upsert idempotente do backfill
- **Resiliência:** simular desconexão do WebSocket e gap de candles; verificar recuperação automática

## 9. Observabilidade

Eventos de log mínimos desta fase: `market_connected`, `market_disconnected`, `market_reconnected`, `gap_detected`, `backfill_started`, `backfill_completed`, `candle_persisted`, `candle_rejected`.

## 10. Decisões pendentes para próximas fases

- Biblioteca numérica para Feature Engine: **decidido** — sem dependências externas (Tablesaw/Smile); implementação própria em Java (ver Fase 1).
