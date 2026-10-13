# Fase 0 — Walking Skeleton — SDD

**Depende de:** nada (primeira fase)
**Módulos:** `market`, `shared`, `configuration`
**Referência:** [SDD.md](../SDD.md) seções 5–17, 73–87

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
- `GET /actuator/health` reporta estado da aplicação e da conexão com o banco (health indicator padrão do Spring Boot — não há indicador customizado para a Binance nesta fase; a disponibilidade do WebSocket é observável via `MarketStatus.websocketConnected`, não via `/actuator/health`)
- `GET /api/v1/market/{symbol}/status` reporta a fase atual (ver seção 10), `websocketConnected`, `historicalDataReady` e contagem de candles
- Frontend mostra preço atual e atualiza sem refresh manual
- API continua respondendo mesmo com frontend fechado

## 8. Testes obrigatórios

- **Unit:** validação de candle (preço negativo, high < low, timestamp fora de ordem)
- **Integration (Testcontainers):** persistência com constraint única, migration aplicada, upsert idempotente do backfill
- **Resiliência:** simular desconexão do WebSocket e gap de candles; verificar recuperação automática

## 9. Observabilidade

Eventos de log mínimos desta fase: `market_connected`, `market_disconnected`, `market_reconnected`, `gap_detected`, `historical_backfill_started`, `historical_backfill_batch`, `historical_backfill_completed`, `candle_persisted`, `candle_rejected`.

## 10. Initial Historical Backfill — mecanismo implementado

Esta seção documenta o comportamento **real implementado** (substitui a descrição original da seção 5/6, que previa apenas "backfill REST em chunks" sem detalhar o estado observável). Consulte também [SDD.md §15–16](../SDD.md).

### 10.1 Máquina de estados (`MarketPhase`)

Por instrumento habilitado, o módulo `market` percorre:

```text
INITIALIZING → CHECKING_HISTORY → BACKFILLING ⇄ RETRYING → HISTORICAL_DATA_READY → LIVE
```

- `INITIALIZING`: instrumento ainda não começou a verificação de histórico.
- `CHECKING_HISTORY`: `HistoricalCoverageChecker` está calculando lacunas entre o início requerido (`BackfillPeriod.resolveStart`, ex. `now - 12 meses`) e agora, alinhados ao boundary do intervalo (`alignToBoundary`) para evitar falsos gaps por precisão de nanossegundos.
- `BACKFILLING`: há lacunas; `HistoricalBackfillService` busca cada lacuna via REST, valida e persiste.
- `RETRYING`: uma tentativa de busca falhou; nova tentativa até `market.backfill.max-retries` (default 3) antes de desistir daquela lacuna.
- `HISTORICAL_DATA_READY`: todas as lacunas requeridas foram cobertas (ou o backfill está desabilitado por config).
- `LIVE`: o WebSocket de candles ao vivo foi iniciado (`MarketDataProvider.streamLive`). Esta etapa **não espera** o histórico terminar perfeitamente — o `MarketStreamingStarter` roda o backfill e a transição para `LIVE` em uma thread virtual dedicada por instrumento, sem bloquear o boot do contexto Spring nem os demais instrumentos.

### 10.2 Algoritmo de verificação de cobertura

`HistoricalCoverageChecker.findMissingRanges(symbol, interval, from, to)` nunca assume cobertura completa apenas por `count > 0`. Ele:

1. Busca os `open_time` existentes no intervalo `[from, to]`.
2. Se vazio, a lacuna é o intervalo inteiro.
3. Se o primeiro candle existente é posterior a `from`, há uma lacuna inicial.
4. Percorre candles consecutivos: se o próximo esperado (`anterior + interval`) não bate com o próximo real, há uma lacuna no meio.
5. Se o último candle existente termina antes de `to`, há uma lacuna final.

Isso cobre os três casos de lacuna (início, meio, fim) — não só "banco vazio = precisa backfill completo".

### 10.3 Idempotência

Reiniciar a aplicação (ou rodar `docker compose up` de novo) não reprocessa candles já persistidos: a constraint `UNIQUE(symbol, interval, open_time)` + a checagem de cobertura acima garantem que apenas os `HistoricalRange`s realmente faltantes são buscados novamente. Validado com Testcontainers: banco vazio, histórico parcial (gap no meio), e re-execução sem duplicar.

### 10.4 Recuperação de gaps no live stream (`GapRecoveryService`)

Se a conexão WebSocket cair e reconectar, o sistema detecta o intervalo perdido (entre o último candle persistido e o candle ao vivo mais recente) e busca via REST antes de retomar a publicação ao vivo — mesmo mecanismo de validação/persistência do backfill inicial, acionado sob demanda em vez de apenas no boot.

### 10.5 API de status (`MarketStatus` / `MarketStatusRegistry`)

```java
public record MarketStatus(
    String symbol,
    MarketPhase phase,
    boolean websocketConnected,
    boolean historicalDataReady,
    long candleCount,
    Instant earliestCandle,
    Instant latestCandle,
    Instant requiredHistoryStart,
    Instant updatedAt
) {}
```

Exposto via:

- `GET /api/v1/market/status` — status de todos os instrumentos configurados.
- `GET /api/v1/market/{symbol}/status` — status de um instrumento.
- WebSocket STOMP, tópico `/topic/market-status/{symbol}` — atualizações empurradas ao dashboard.

`MarketStatusRegistry` é atualizado de duas formas, por responsabilidade do caller (não pelo `CandlePersister`, que foi mantido com responsabilidade única de validar+persistir um candle):

- **Ingestão ao vivo** (`CandleIngestionService`): chama `candlePersisted()` a cada candle (1/min por instrumento — sem custo de performance relevante).
- **Lotes de backfill/gap-recovery** (`HistoricalBackfillService`, `GapRecoveryService`): chamam `refreshCounts()` uma vez por lote, não por candle (lotes de centenas/milhares de candles tornariam updates por-candle custosos sem benefício observável).

### 10.6 Configuração

```yaml
market:
  backfill:
    max-retries: ${BACKFILL_MAX_RETRIES:3}
  instruments:
    - symbol: BTCUSDT
      interval: 1m
      enabled: true
      historical-backfill:
        enabled: ${BACKFILL_ENABLED:true}
        period: ${BACKFILL_PERIOD:12-months}
```

`BackfillPeriod` aceita `12-months` (default), e formatos menores como `2-days` para validação rápida em desenvolvimento (não precisa esperar backfill de 12 meses reais para testar o fluxo end-to-end).

### 10.7 Degradação graciosa

Backfill desabilitado (`historical-backfill.enabled=false`) não impede a ingestão ao vivo: o instrumento pula direto para `HISTORICAL_DATA_READY` (sem dados históricos) e depois `LIVE`. Falhas de rede durante o backfill (até esgotar `max-retries`) também não bloqueiam a transição para `LIVE` dos demais instrumentos, nem impedem que o WebSocket ao vivo do próprio instrumento comece assim que o backfill (com ou sem sucesso total) terminar.

## 11. Decisões pendentes para próximas fases

- Biblioteca numérica para Feature Engine: **decidido** — sem dependências externas (Tablesaw/Smile); implementação própria em Java (ver Fase 1).
