# BTC Intelligence Cockpit — Software Design Document

**Projeto:** BTC Opportunity Assistant  
**Produto:** BTC Intelligence Cockpit  
**Versão:** 1.2  
**Status:** Implementation-ready — baseline final consolidada  
**Stack:** Java 25 LTS / Spring Boot 4.x / PostgreSQL 18 / Next.js 16 / Node.js 24 LTS / Ollama  
**Arquitetura:** Monólito modular inicialmente  
**Multi-asset:** Arquitetura pronta para extensão; MVP roda com um único instrumento ativo (BTCUSDT)  
**Trading automático:** Não permitido

---

## 1. Objetivo

Construir um sistema pessoal de inteligência para criptoativos, tendo BTC/USDT como primeiro instrumento, que acompanha o mercado em tempo real, identifica eventos quantitativos relevantes, calcula contexto e risco, compara o contexto atual com situações históricas, produz oportunidades classificadas, utiliza IA local para interpretar evidências, alerta o usuário, registra decisões e operações e avalia posteriormente resultados e oportunidades ignoradas.

O pipeline (market → feature → event → opportunity → risk → historical → agent) deve ser agnóstico de instrumento: nenhuma etapa deve ter "BTC" hardcoded em sua lógica. Isso permite adicionar outros pares (ex. ETHUSDT, SOLUSDT) futuramente apenas por configuração, sem reescrever o pipeline.

O sistema **não executa ordens**. A decisão final é sempre do usuário.

Pergunta central:

> "Dado o que sabemos agora, existe uma situação suficientemente interessante, explicável e historicamente sustentada para merecer minha atenção?"

---

## 2. Princípios

### 2.1 Quant primeiro, IA depois

```text
Market Data
    ↓
Features
    ↓
Events
    ↓
Opportunity
    ↓
Risk
    ↓
Historical Context
    ↓
Agent
    ↓
Explanation
```

Nunca:

```text
Market Data
    ↓
LLM
    ↓
Trading Decision
```

O LLM interpreta evidências calculadas pelo sistema.

### 2.2 Sem lookahead

Uma decisão no instante `T` somente pode utilizar informações disponíveis até `T`.

Dados posteriores a `T` só podem ser usados para avaliar o resultado da decisão.

### 2.3 Ausência de oportunidade é válida

Estados:

```text
NONE
WATCH
SETUP
STRONG_SETUP
```

`NONE` é resultado normal.

### 2.4 Auditabilidade

Todo alerta deve poder responder:

- qual evento o originou;
- qual preço existia naquele momento;
- quais features foram usadas;
- qual score foi calculado;
- qual risco foi calculado;
- qual contexto histórico foi encontrado;
- qual versão do engine foi utilizada;
- qual configuração foi utilizada;
- qual versão do agente/prompt foi utilizada.

### 2.5 IA é enriquecimento

Se Ollama estiver indisponível, o sistema quantitativo continua funcionando.

### 2.6 Agnóstico de instrumento (multi-asset readiness)

`symbol` é uma dimensão de configuração, não uma constante de código.

Candles, features, eventos, oportunidades, riscos, contexto histórico e alertas já carregam `symbol` desde o modelo de dados (seções 10, 21, 42).

A ingestão de mercado deve ser abstraída por uma interface (`MarketDataProvider`, seção 12), permitindo trocar ou adicionar fontes/exchanges sem alterar o restante do pipeline.

No MVP, apenas BTCUSDT está habilitado. Adicionar um novo ativo deve ser uma mudança de configuração (seções 9/15), não uma mudança estrutural.

---

## 3. Não objetivos

Não implementar no MVP:

- execução automática de ordens;
- API key privada de trading;
- withdrawals;
- alavancagem;
- position sizing automático;
- stop automático;
- múltiplos agentes independentes;
- Kafka;
- Kubernetes;
- Redis sem necessidade comprovada;
- vector database;
- embeddings para similaridade;
- múltiplos ativos simultâneos no MVP (apenas BTCUSDT ativo, demais desabilitados por configuração);
- centenas de ativos;
- previsão determinística de preço.

---

# 4. Stack tecnológica

## 4.1 Backend

- Java 25 LTS
- Spring Boot 4.x estável
- Spring Framework 7.x
- Maven
- Spring Web (stack servlet; WebSocket para streaming do cockpit via `WebSocketHandler`/STOMP)
- Spring Data JPA
- Hibernate
- Flyway
- Jackson
- Bean Validation
- Spring Actuator
- Micrometer
- JUnit 5
- Mockito
- Testcontainers

Nota: não utilizar Spring WebFlux no MVP. Misturar WebFlux (não bloqueante) com Spring Data JPA (bloqueante) sem isolamento cuidadoso de schedulers pode travar o event loop sob carga. Servlet stack simples é suficiente para o volume de dados deste produto (ver ADR-009).

Decisão encerrada (ADR-013): implementação própria em Java para o Feature Engine, sobre `BigDecimal`, com componentes isolados e testáveis (`ReturnCalculator`, `VolatilityCalculator`, `VolumeAnalyzer`, `MomentumCalculator`, `RollingWindow`, `ZScoreCalculator` — ver Fase 1). Não utilizar `Tablesaw`/`Smile` no MVP.

## 4.2 Frontend

- Node.js 24 LTS
- Next.js 16.x Active LTS
- React
- TypeScript
- Tailwind CSS
- pnpm
- ESLint
- Prettier

## 4.3 Banco

- PostgreSQL 18.x
- usar o patch estável mais recente da linha 18

## 4.4 IA

- Ollama local
- provider abstrato no backend (`AgentProvider`, seção 33)
- modelo local configurável
- avaliar `Spring AI` (abstração `ChatClient` com suporte a Ollama e OpenAI) como implementação de `OllamaAgentProvider`, para reduzir código de integração próprio

## 4.5 Infraestrutura

- Docker
- Docker Compose
- Git

### Política de versões

Para tecnologias que possuem LTS, utilizar a LTS vigente.

Para tecnologias sem conceito de LTS, utilizar a versão estável atual compatível com a baseline.

Não utilizar versões alpha, beta, milestone, RC ou SNAPSHOT.

---

# 5. Arquitetura

O projeto será um **monólito modular Spring Boot**.

Não criar vários microserviços no início.

```text
btc-opportunity-assistant/
│
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/btcassistant/
│       │   │   ├── market/
│       │   │   ├── feature/
│       │   │   ├── event/
│       │   │   ├── opportunity/
│       │   │   ├── risk/
│       │   │   ├── historical/
│       │   │   ├── agent/
│       │   │   ├── alert/
│       │   │   ├── decision/
│       │   │   ├── operation/
│       │   │   ├── research/
│       │   │   ├── configuration/
│       │   │   └── shared/
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       └── test/
│
├── frontend/
├── research/
├── data/
├── infra/
├── docs/
├── tests/
├── docker-compose.yml
├── README.md
└── .env.example
```

Os módulos são limites de domínio, não processos independentes.

---

# 6. Módulos do backend

### market

- `MarketDataProvider` (abstração de fonte de dados; implementação inicial `BinanceMarketDataProvider`);
- suporte a múltiplos instrumentos configuráveis (um único ativo habilitado no MVP);
- conexão Binance;
- WebSocket;
- REST backfill;
- candles;
- reconexão;
- gap detection;
- recuperação.

### feature

- retornos;
- volume;
- volatilidade;
- momentum;
- extremos;
- features versionadas.

### event

- detecção de eventos;
- persistência;
- versionamento.

### opportunity

- classificação;
- score;
- direção;
- classificação de oportunidade.

### risk

- avaliação de risco;
- nível;
- razões.

### historical

- features históricas;
- similaridade;
- resultados futuros;
- MFE/MAE;
- contexto histórico.

### agent

- AgentProvider;
- OllamaProvider;
- Agent Orchestrator;
- timeout;
- retry;
- validação.

### alert

- lifecycle;
- deduplicação;
- cooldown;
- expiração.

### decision

- BUY;
- SELL;
- IGNORE;
- WAIT.

### operation

- operações;
- screenshots;
- OCR/Vision futuro;
- P&L.

---

# 7. M0 — Walking Skeleton

Antes de implementar inteligência:

```text
Binance
   ↓
WebSocket
   ↓
BTCUSDT 1m
   ↓
PostgreSQL
   ↓
Spring Boot API
   ↓
WebSocket
   ↓
Next.js
```

Não implementar em M0:

- Feature Engine;
- Event Detector;
- Opportunity Engine;
- Risk Engine;
- Historical Analyzer;
- Agent;
- Ollama.

Objetivo: provar infraestrutura, streaming, persistência e frontend.

---

# 8. M0 — Definition of Done

M0 termina quando:

- `docker compose up` funciona;
- PostgreSQL sobe;
- Flyway executa;
- Spring Boot sobe;
- Next.js sobe;
- Market module conecta ao Binance;
- BTCUSDT 1m chega;
- candle é validado;
- candle é persistido;
- duplicatas são tratadas;
- frontend mostra preço;
- frontend atualiza em tempo real;
- WebSocket reconecta;
- gaps são detectados;
- REST backfill funciona;
- health checks funcionam;
- logs estruturados funcionam;
- testes básicos passam;
- README explica execução.

---

# 9. Market Data

Instrumentos são configuráveis, não hardcoded. O domínio nunca modela "Bitcoin" diretamente — trabalha com o conceito genérico `Instrument`:

```java
public record Instrument(
    String symbol,      // BTCUSDT
    String baseAsset,   // BTC
    String quoteAsset,  // USDT
    String exchange,    // BINANCE
    String interval,    // 1m
    boolean enabled
) {}
```

Nenhuma classe de domínio deve se chamar `BitcoinCandle`, `BitcoinFeature`, `BitcoinEvent` etc. — sempre `Candle`, `FeatureSnapshot`, `DetectedEvent`, `Opportunity`, `RiskAssessment`, `Alert`, carregando `Instrument`/`symbol` como contexto, nunca como premissa arquitetural.

Configuração inicial:

```yaml
market:
  instruments:
    - symbol: BTCUSDT
      base-asset: BTC
      quote-asset: USDT
      exchange: BINANCE
      interval: 1m
      enabled: true
    # - symbol: ETHUSDT
    #   base-asset: ETH
    #   quote-asset: USDT
    #   exchange: BINANCE
    #   interval: 1m
    #   enabled: false
```

No MVP apenas um instrumento está habilitado (`BTCUSDT`). Habilitar um novo ativo é alteração de configuração; o código de market/feature/event/opportunity não deve ser alterado para isso.

Fonte:

```text
Binance public market data
```

Abstraída por `MarketDataProvider` (seção 12), permitindo trocar/adicionar exchange no futuro sem impacto no restante do pipeline.

Timeframe principal:

```text
1m
```

Timeframes derivados:

```text
5m
15m
30m
1h
4h
12h
24h
```

Nenhuma credencial privada é necessária.

---

# 10. Candle

Modelo:

```text
Candle
- id
- symbol
- interval
- openTime
- closeTime
- open
- high
- low
- close
- volume
- quoteVolume
- tradeCount
- source
- createdAt
```

Todos os timestamps são UTC.

Constraint:

```text
UNIQUE(symbol, interval, open_time)
```

---

# 11. Precisão

Não utilizar `double` para valores financeiros.

Utilizar:

```java
BigDecimal
```

para:

- preço;
- volume;
- notional;
- P&L;
- taxas.

Utilizar:

```java
Instant
```

para timestamps.

Utilizar:

```java
Duration
```

para durações.

---

# 12. Market Data Provider

Interface (sem tipos reativos — consistente com a decisão de não usar WebFlux, ADR-009):

```java
public interface MarketDataProvider {

    void streamLive(Instrument instrument, CandleListener listener);

    List<Candle> fetchHistorical(Instrument instrument, Instant from, Instant to);

}

public interface CandleListener {
    void onCandle(Candle candle);
}
```

`streamLive` roda em thread própria do client WebSocket; cada candle recebido é entregue via `onCandle` (callback), que persiste (JPA, bloqueante) e publica para as sessões WebSocket do cockpit. Nenhuma parte do pipeline depende de Reactor/WebFlux.

Implementação inicial: `BinanceMarketDataProvider`.

Futura: outras exchanges (ex. `CoinbaseMarketDataProvider`), sem alterar o restante do pipeline.

Utilizar:

- WebSocket para live;
- REST para histórico/backfill, respeitando limites de taxa da Binance (HTTP 429 / `Retry-After`) com backoff entre chunks.

Fluxo:

```text
WebSocket
    ↓
Parser
    ↓
Validator
    ↓
Candle
    ↓
Persistence
    ↓
Event publication
```

---

# 13. Reconexão

Exponential backoff:

```text
1s
2s
4s
8s
16s
30s
30s
...
```

Valores configuráveis.

Eventos:

```text
websocket_connected
websocket_disconnected
websocket_reconnecting
```

---

# 14. Gap recovery

Detectar gaps pela sequência temporal.

Para 1m:

```text
T
T + 1m
T + 2m
T + 3m
```

Ao detectar:

```text
Live stream
    ↓
Gap detected
    ↓
REST backfill
    ↓
Validate
    ↓
Persist
    ↓
Resume
```

Recuperação idempotente.

---

# 15. Backfill inicial

Primeiro setup, por instrumento habilitado:

```text
12 meses
1m
```

Aproximadamente 525.600 candles por instrumento.

Configuração (por instrumento, dentro de `market.instruments`):

```yaml
market:
  instruments:
    - symbol: BTCUSDT
      enabled: true
      historical-backfill:
        enabled: true
        interval: 1m
        period: 12-months
```

Suportar futuramente:

- 3 meses;
- 6 meses;
- 12 meses;
- 24 meses;
- custom.

---

# 16. Backfill incremental

Usar chunks:

```text
Chunk
 ↓
REST
 ↓
Validate
 ↓
Upsert
 ↓
Next chunk
```

Respeitar rate limit da Binance entre chunks (backoff ao receber 429); nunca disparar todos os chunks em paralelo sem controle.

Executar novamente não pode duplicar candles.

---

# 17. Integridade dos candles

Validar:

- timestamp;
- ordem temporal;
- preço > 0;
- volume >= 0;
- high >= low;
- high >= open;
- high >= close;
- low <= open;
- low <= close;
- duplicatas;
- gaps.

Candle inválido não deve entrar silenciosamente.

---

# 18. Feature Engine

### Returns

```text
return_1m
return_5m
return_15m
return_30m
return_1h
return_4h
return_12h
return_24h
```

### Volume

```text
volume_sma
volume_ratio
volume_zscore
```

### Volatilidade

```text
volatility_15m
volatility_1h
volatility_4h
volatility_24h
```

### Extremos

```text
distance_recent_high_1h
distance_recent_high_4h
distance_recent_high_24h
distance_recent_low_1h
distance_recent_low_4h
distance_recent_low_24h
```

### Momentum

```text
momentum_5m
momentum_15m
momentum_1h
momentum_4h
```

### Estrutura

```text
drawdown_from_recent_high
recovery_from_recent_low
```

---

# 19. Feature versioning

Toda feature calculation possui `featureVersion`.

Mudou fórmula → nova versão.

Resultados históricos não podem ter seu significado alterado silenciosamente.

---

# 20. Event Detector

Eventos:

```text
RAPID_DROP
RAPID_RISE
VOLUME_SPIKE
VOLATILITY_EXPANSION
VOLATILITY_CONTRACTION
LOCAL_LOW_APPROACH
LOCAL_HIGH_APPROACH
RECOVERY
FAILED_RECOVERY
FAILED_BREAKOUT
BREAKOUT
BREAKDOWN
```

---

# 21. Event model

```text
Event
- eventId
- symbol
- eventType
- eventTime
- price
- featuresSnapshot
- magnitude
- duration
- engineVersion
- createdAt
```

Eventos são imutáveis.

---

# 22. Opportunity Engine

Transforma contexto quantitativo em classificação.

Estados:

```text
NONE
WATCH
SETUP
STRONG_SETUP
```

Direções:

```text
BUY
SELL
NEUTRAL
```

Não executa operações.

---

# 23. Score

Score:

```text
0–100
```

Threshold inicial:

```text
0–49   NONE
50–64  WATCH
65–79  SETUP
80–100 STRONG_SETUP
```

Configuração:

```yaml
opportunity:
  watch-threshold: 50
  setup-threshold: 65
  strong-setup-threshold: 80
```

Thresholds são experimentais.

---

# 24. Score explicável

Componentes possíveis:

```text
eventStrength
historicalSupport
momentumContext
volumeConfirmation
volatilityContext
riskPenalty
```

Cada componente deve ser reproduzível/auditável.

## Regra de score quando um componente está UNKNOWN

Um componente indisponível (ex. `historicalSupport = UNKNOWN` antes da Fase 4 existir, ou amostra insuficiente) **nunca** é convertido em `0`, neutro ou qualquer valor implícito.

1. Cada componente tem peso definido pela configuração versionada (`configVersion`).
2. Componentes `UNKNOWN` não participam do numerador do score.
3. O peso dos componentes disponíveis é renormalizado sobre o peso total efetivamente disponível:

```text
score = contribuição_disponível / peso_disponível × 100
```

4. O score resultante permanece entre `0` e `100`.
5. A ausência de evidência não é mascarada pelo score. O resultado preserva explicitamente:

```json
{
  "historicalSupport": "UNKNOWN",
  "evidenceQuality": "LIMITED",
  "availableEvidenceWeight": 0.80
}
```

6. `historicalSupport = UNKNOWN` não significa evidência negativa nem neutra — significa ausência de evidência histórica suficiente para aquele contexto.
7. A classificação (`NONE/WATCH/SETUP/STRONG_SETUP`) continua determinada apenas pelos thresholds configurados — **não existe regra implícita que limite a classificação a `WATCH` só porque `historicalSupport` está `UNKNOWN`**.
8. Ainda assim, a qualidade da evidência permanece explicitamente limitada: um score alto com componente ausente não tem a mesma qualidade de um score com todos os componentes disponíveis.
9. `availableEvidenceWeight` representa o percentual do peso total configurado que foi efetivamente usado no cálculo.
10. `confidence`/`evidenceQuality` refletem a ausência de componentes relevantes. Quando houver componente crítico `UNKNOWN`, a qualidade mínima é `LIMITED`.
11. O agente (LLM) nunca preenche, estima ou infere componentes ausentes — recebe `UNKNOWN` e deve explicá-lo como limitação da evidência, nunca disfarçá-lo.

### Exemplo

```text
event-strength       = 20
historical-support   = 30 → UNKNOWN
momentum-context     = 15
volume-confirmation  = 15
volatility-context   = 10
risk-penalty         = 20
```

Peso disponível: `20+15+15+10+20 = 80`. Se as contribuições disponíveis somam 64 pontos:

```text
score = 64 / 80 × 100 = 80

classification          = STRONG_SETUP
historicalSupport       = UNKNOWN
availableEvidenceWeight = 0.80
evidenceQuality         = LIMITED
```

Comportamento intencional: o sistema separa **força quantitativa do sinal**, **quantidade de evidência disponível**, **qualidade da evidência** e **confiança da interpretação**. Um score alto não implica evidência completa. Esta regra é determinística, testável, e independente do LLM.

---

# 25. Risk Engine

Responsabilidade:

> classificar o risco/contexto da oportunidade.

Saída:

```text
LOW
MEDIUM
HIGH
UNKNOWN
```

Não calcular position sizing no MVP.

---

# 26. Risk inputs

- volatilidade;
- magnitude;
- drawdown;
- recovery;
- distância de extremos;
- velocidade;
- volume;
- qualidade da evidência;
- tamanho da amostra;
- comportamento anormal;
- conflito de sinais.

Dados insuficientes → `UNKNOWN`.

---

# 27. Historical Analyzer

Encontrar situações historicamente semelhantes ao contexto atual.

Features:

```text
eventType
magnitude
duration
volumeRatio
volatility
momentum
distanceToExtreme
```

Não buscar apenas preço absoluto.

---

# 28. Historical Feature Store

Features históricas devem ser pré-calculadas e persistidas.

Primeira implementação:

```text
PostgreSQL
+
SQL filtering
+
Java processing
```

Não usar inicialmente:

- vector DB;
- embeddings;
- FAISS.

---

# 29. Similarity

Features normalizadas + distância ponderada.

Retornar:

```text
sampleSize
similarEvents
similarityScore
```

Pesos configuráveis e versionados.

---

# 30. Historical outcomes

Para cada evento histórico:

```text
5m
15m
30m
1h
2h
4h
12h
24h
```

Avaliar:

```text
futureReturn
MFE
MAE
timeToTarget
timeToStop
targetBeforeStop
```

O modelo deve registrar explicitamente quando o resultado ficou **completamente conhecido**, não apenas quando o evento ocorreu:

```text
eventTime       = 10:00
horizon         = 1h
evaluationEnd   = 11:00   # eventTime + horizon
```

A definição de `target`/`stop` é versionada — não assumir arbitrariamente `target = +X% / stop = -Y%` sem definição experimental:

```yaml
research:
  outcome:
    target-definition-version: "1.0"
    stop-definition-version: "1.0"
```

Mudar a metodologia de target/stop gera nova versão; resultados antigos permanecem associados à versão anterior.

---

# 31. No-lookahead histórico

Para uma decisão/alerta no instante `T`, um evento histórico candidato só é elegível se:

```text
candidate.eventTime < T                    (estrito, não <=)
candidate.eventId != currentEvent.eventId  (nunca comparar o evento consigo mesmo)
```

E qualquer outcome usado desse candidato só é elegível se:

```text
candidate.outcome.evaluationEnd <= T
```

Não basta o evento ter ocorrido antes de `T` — o resultado (outcome) também precisa já estar **completamente conhecido** em `T`. Exemplo: evento histórico às 10:00, outcome de 1h conhecido às 11:00, outcome de 4h conhecido às 14:00. Avaliando em `T = 12:00`: o outcome de 1h pode ser usado, o de 4h não pode.

Resultados futuros (outcomes com `evaluationEnd > T`) só podem ser usados posteriormente, para avaliação — nunca como contexto de decisão.

---

# 32. Agent Orchestrator

O LLM não roda a cada minuto.

Fluxo:

```text
Event
 ↓
Opportunity
 ↓
Score >= WATCH
 ↓
Agent Orchestrator
 ↓
Ollama
```

Responsabilidades:

- decidir quando chamar;
- montar contexto;
- chamar provider;
- validar;
- armazenar;
- timeout;
- retry;
- fallback.

---

# 33. AgentProvider

Interface:

```java
public interface AgentProvider {

    AgentAnalysis analyze(AgentContext context);

}
```

Implementação inicial:

```text
OllamaAgentProvider
```

Futura:

```text
OpenAIAgentProvider
```

O domínio não conhece diretamente o Ollama.

---

# 34. Agent timeout/retry

```yaml
agent:
  timeout: 30s
  max-retries: 1
```

O Agent nunca bloqueia o pipeline quantitativo.

---

# 35. Graceful degradation

Se Ollama cair:

```text
Market = OK
Features = OK
Events = OK
Opportunity = OK
Risk = OK
Historical = OK
Agent = UNAVAILABLE
Dashboard = OK
```

Não gerar falsa resposta.

---

# 36. Agent context

```json
{
  "instrument": {
    "symbol": "BTCUSDT",
    "baseAsset": "BTC",
    "quoteAsset": "USDT",
    "exchange": "BINANCE",
    "interval": "1m"
  },
  "market": {},
  "features": {},
  "event": {},
  "opportunity": {},
  "risk": {},
  "historicalContext": {},
  "recentAlerts": []
}
```

`InstrumentContext` é sempre explícito — o agente interpreta o instrumento como dado de entrada, nunca como premissa fixa de código ou de prompt.

Na primeira versão o agente não busca fontes externas.

---

# 37. Market Analyst

Um único agente lógico:

```text
Market Analyst
```

Não utilizar "BTC Analyst" como nome arquitetural — o agente recebe o instrumento via `InstrumentContext` e nunca assume Bitcoin no prompt ou no código.

Tools:

```text
getCurrentMarketState()
getMarketFeatures()
getRecentEvents()
searchSimilarHistoricalEvents()
getOpportunityContext()
getRiskContext()
getRecentAlerts()
```

Não criar múltiplos agentes inicialmente.

---

# 38. Agent output

Structured JSON:

```json
{
  "classification": "SETUP",
  "direction": "BUY",
  "score": 78,
  "confidence": "MEDIUM",
  "summary": "Queda acelerada com evidência histórica favorável.",
  "evidence": [],
  "historicalContext": {
    "sampleSize": 426,
    "targetBeforeStopProbability": 0.69
  },
  "risk": {
    "level": "MEDIUM",
    "reasons": []
  },
  "invalidations": []
}
```

Valores são apenas ilustrativos.

---

# 39. Regras do agente

O agente:

- não inventa métricas;
- não inventa notícias;
- não inventa preços;
- não inventa probabilidades;
- não executa operações;
- não substitui cálculos determinísticos;
- não cria oportunidade inexistente;
- declara dados ausentes.

---

# 40. Linguagem

Não utilizar:

> "Compre agora."

Preferir:

> "Setup de compra que merece avaliação."

ou:

> "Contexto favorável a uma possível entrada, com risco médio."

---

# 41. Alert lifecycle

```text
DETECTED
    ↓
ANALYZING
    ↓
CREATED
    ↓
ACTIVE
    ↓
EXPIRED
    ↓
EVALUATED
```

---

# 42. Alert model

```text
alertId
opportunityId
createdAt
symbol
direction
score
classification
confidence
riskLevel
priceAtAlert
marketSnapshot
featureSnapshot
historicalSnapshot
agentAnalysis
executionMode
engineVersion
featureVersion
configVersion
agentVersion
status
```

`executionMode` (`LIVE`/`BACKTEST`) identifica se o alerta veio do pipeline em tempo real ou de uma simulação de backtest (ver seção 59). O dashboard operacional só exibe `LIVE` por padrão; dados de `BACKTEST` nunca aparecem misturados com `LIVE` em nenhuma consulta operacional.

Alertas são imutáveis.

---

# 43. Alert deduplication

Implementar:

- identificação do evento;
- cooldown;
- deduplicação;
- agrupamento;
- expiração.

```yaml
alerts:
  cooldown: 15m
```

---

# 44. Alert fatigue

Medir:

```text
alertsPerHour
alertsPerDay
duplicateAlertRate
ignoredAlertRate
actedAlertRate
```

Preferir não alertar a gerar alertas ruins.

---

# 45. Dashboard

Nome:

**BTC Intelligence Cockpit**

Objetivo:

> entender o mercado em menos de 10 segundos.

---

# 46. Dashboard — market

Mostrar:

```text
BTC/USDT
Preço
1m
5m
1h
24h
```

---

# 47. Dashboard — chart

Mostrar:

- candles;
- eventos;
- alertas;
- oportunidade atual.

---

# 48. Dashboard — opportunity

Exemplo:

```text
SETUP
BUY

Score: 78
Confidence: MEDIUM
Risk: MEDIUM
```

Mostrar evidências, histórico e invalidações.

---

# 49. Dashboard — historical

Mostrar:

```text
Similar historical events
Sample size
Target before stop
Median return
Median MFE
Median MAE
```

Sempre mostrar o tamanho da amostra.

---

# 50. Dashboard — no opportunity

Mostrar:

```text
NO ACTIVE OPPORTUNITY
```

Isso é comportamento normal.

---

# 51. Real-time frontend

Eventos:

```text
market_update
feature_update
event_detected
opportunity_created
alert_created
alert_expired
operation_updated
```

WebSocket é a opção preferencial para o cockpit.

---

# 52. User Decision

Decisões:

```text
BUY
SELL
IGNORE
WAIT
```

Uma decisão não implica operação.

---

# 53. Operation

```text
operationId
alertId
side
quantity
price
notional
timestamp
screenshotPath
source
confidence
```

## Trade / Position (agrupamento de operações)

Uma `Operation` representa uma execução isolada (entrada ou saída). Para evitar que a arquitetura trate cada operação como independente, operações relacionadas são agrupadas em `Trade`/`Position`:

```text
Decision
    ↓
Trade / Position
    ├── Entry Operation
    └── Exit Operation
```

```text
tradeId
decisionId
entryOperationId
exitOperationId (nullable até o trade ser fechado)
status          (OPEN/CLOSED)
```

O P&L (seção 55) pertence ao `Trade`, embora seja calculado a partir das duas `Operation`. Não transformar isso em um módulo grande agora — apenas evitar a limitação de operações desacopladas.

---

# 54. Screenshot workflow

Futuro:

```text
Alert
 ↓
User clicks operation
 ↓
Upload screenshot
 ↓
OCR/Vision extraction
 ↓
User confirms
 ↓
Operation persisted
```

OCR nunca é verdade sem confirmação.

---

# 55. P&L

Calculado por `Trade` (seção 53), a partir do par entry/exit de `Operation`:

```text
grossPnl
fees
estimatedSlippage
netPnl
returnPct
holdingTime
```

---

# 56. Personal Trading Memory

```text
Market Event
    ↓
Opportunity
    ↓
Alert
    ↓
User Decision
    ↓
Operation
    ↓
Result
```

Também registrar:

```text
Alert
 ↓
IGNORE
 ↓
Future market outcome
```

---

# 57. Separação de fatos

Não confundir:

```text
System:
SETUP / BUY
```

com:

```text
User:
IGNORE
```

e:

```text
Market:
+2.3%
```

São fatos independentes.

---

# 58. Avaliação de alertas

Avaliar:

```text
future_5m
future_15m
future_30m
future_1h
future_4h
future_12h
future_24h
MFE
MAE
targetBeforeStop
```

---

# 59. Backtesting

Backtest cronológico:

```text
T1 → decision
T2 → decision
T3 → decision
...
```

Nunca utilizar futuro para produzir a decisão. Reproduz a mesma lógica do pipeline em tempo real: candle disponível em `T` → features até `T` → evento → oportunidade → risco → contexto histórico disponível em `T` → decisão/alerta simulado. Somente depois (`T + horizonte`) o resultado futuro é usado para avaliação.

## Execution Mode

Todo registro operacional/de pesquisa carrega `executionMode` (`LIVE` ou `BACKTEST`; futuro: `REPLAY`, `SIMULATION`), propagado por `opportunity`, `alert`, `decision` e `evaluation`. Dados de `BACKTEST` nunca contaminam consultas/telas de `LIVE` — o Research consulta `BACKTEST` explicitamente; o dashboard operacional nunca.

---

# 60. Teste obrigatório de lookahead

Executar:

```text
decision(T)
```

Adicionar candles futuros.

Executar novamente:

```text
decision(T)
```

Resultado deve ser idêntico.

Se mudar:

```text
FAIL
```

---

# 61. Determinismo

Decisão quantitativa reproduzível com:

```text
marketData
engineVersion
featureVersion
configVersion
```

---

# 62. Configuration versioning

```yaml
rapid-drop:
  window: 15m
  threshold: -0.02

volume-spike:
  zscore: 2.5

opportunity:
  watch-threshold: 50
  setup-threshold: 65
  strong-setup-threshold: 80
```

Toda decisão registra `configVersion`.

---

# 63. Regimes

Futuro:

```text
TREND_UP
TREND_DOWN
RANGE
HIGH_VOLATILITY
LOW_VOLATILITY
```

Não bloquear M0/M1.

---

# 64. Research

```text
research/
└── experiments/
    ├── EXP-001/
    │   ├── README.md
    │   ├── config.yaml
    │   ├── results.json
    │   └── conclusion.md
    └── ...
```

Cada experimento registra:

- hypothesis;
- dataset;
- period;
- features;
- parameters;
- criteria;
- results;
- limitations;
- conclusion;
- next experiment.

---

# 65. Primeira hipótese

> Eventos extremos de preço acompanhados de alterações relevantes de volume e volatilidade podem apresentar padrões históricos suficientemente consistentes para gerar oportunidades classificáveis.

Isso é uma hipótese, não uma regra de trading.

---

# 66. EXP-001

Avaliar movimentos rápidos de alta/baixa.

Horizontes:

```text
5m
15m
30m
1h
2h
4h
12h
24h
```

Métricas:

```text
futureReturn
MFE
MAE
targetBeforeStop
timeToTarget
```

Separar por:

```text
magnitude
volume
volatility
regime
```

---

# 67. Out-of-sample

Não considerar estratégia validada somente porque funciona no dataset usado para criá-la.

Utilizar:

- holdout;
- walk-forward;
- OOS.

---

# 68. Versionamento

Registrar:

```text
engineVersion
featureVersion
configVersion
historicalVersion
agentVersion
promptVersion
targetDefinitionVersion
stopDefinitionVersion
```

Mudanças não podem apagar resultados antigos.

---

# 69. API

```text
GET  /health
GET  /api/v1/market/btcusdt
GET  /api/v1/market/candles
GET  /api/v1/features/latest
GET  /api/v1/events
GET  /api/v1/opportunities
GET  /api/v1/alerts
GET  /api/v1/alerts/{id}
POST /api/v1/decisions
POST /api/v1/operations
POST /api/v1/operations/{id}/screenshot
GET  /api/v1/performance
```

API versionada.

---

# 70. Contratos

Usar DTOs para API.

Não expor entidades JPA diretamente.

```text
JPA Entity
   ↓
Domain
   ↓
DTO
   ↓
API
```

Quando apropriado, utilizar Java records:

```java
public record OpportunityResult(
    Direction direction,
    Classification classification,
    BigDecimal score,
    Confidence confidence,
    RiskLevel risk
) {}
```

---

# 71. Banco

PostgreSQL armazena:

- candles;
- features;
- events;
- opportunities;
- risks;
- historical context;
- alerts;
- analyses;
- decisions;
- operations;
- configurations.

Parquet pode ser usado para research/export.

## Estratégia relacional + JSONB (não usar NoSQL separado)

Decisão (ADR-010): PostgreSQL único, sem banco NoSQL adicional.

Campos com formato estável e usados em filtros/joins (símbolo, timestamps, ids, preço, score, status) ficam em colunas tipadas, com constraints e foreign keys.

Campos que mudam de forma com frequência (snapshot de features, breakdown do score, evidências/análise do agente) ficam em colunas `JSONB` dentro das próprias tabelas relacionais — assim o schema evolui sem migration obrigatória para cada novo campo desses, sem abrir mão de integridade referencial, transações ACID e precisão numérica (`NUMERIC`) nas partes que realmente não podem ficar soltas.

NoSQL dedicado (ex. MongoDB) foi descartado por perder: foreign keys entre event → opportunity → alert → decision → operation; transações ACID ao registrar decisão + operação + P&L; precisão numérica financeira madura no ecossistema; e por não eliminar a necessidade de schema — apenas move a validação do banco para o código da aplicação.

Se o volume de candles crescer a ponto de PostgreSQL puro não performar bem (não é o caso em ~525k linhas/ano), considerar a extensão `TimescaleDB` sobre o mesmo PostgreSQL antes de cogitar um banco separado.

---

# 72. Migrations

Utilizar Flyway.

Nunca modificar migration já aplicada.

Exemplo:

```text
V1__create_candles.sql
V2__create_features.sql
V3__create_events.sql
V4__create_opportunities.sql
...
```

---

# 73. Observabilidade

Utilizar Spring Actuator + Micrometer.

Logs estruturados.

Eventos:

```text
market_connected
market_disconnected
market_reconnected
gap_detected
backfill_started
backfill_completed
candle_persisted
candle_rejected
event_detected
opportunity_created
agent_started
agent_completed
agent_failed
alert_created
operation_created
```

---

# 74. Health

Distinguir:

```text
API
DATABASE
BINANCE
MARKET_ENGINE
OLLAMA
```

Ollama indisponível não derruba health geral do sistema.

---

# 75. Graceful degradation

### Binance offline

```text
Market = STALE
Dashboard = AVAILABLE
Historical = AVAILABLE
```

### Ollama offline

```text
Quantitative = AVAILABLE
AI = UNAVAILABLE
```

### Historical unavailable

```text
Opportunity = AVAILABLE
HistoricalContext = UNAVAILABLE
```

---

# 76. Segurança e qualidade contínua

MVP:

- sem exchange credentials;
- sem private API;
- sem withdrawal;
- sem trading API.

Secrets em `.env`.

Nunca commitar secrets.

Screenshots fora do Git e com acesso controlado.

## Dependabot

Habilitado desde a Fase 0, cobrindo os ecossistemas existentes no monorepo:

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: maven
    directory: /backend
    schedule: { interval: weekly }
  - package-ecosystem: npm
    directory: /frontend
    schedule: { interval: weekly }
  - package-ecosystem: docker
    directory: /
    schedule: { interval: weekly }
  - package-ecosystem: github-actions
    directory: /
    schedule: { interval: weekly }
```

Atualizações de dependências com vulnerabilidade conhecida têm prioridade sobre qualquer outra tarefa.

## Análise estática (SonarCloud/SonarQube)

Obrigatório a partir da Fase 0, como gate de CI (não apenas relatório informativo):

- Java: plugin `sonar-maven-plugin`, com Jacoco para cobertura.
- Frontend: `sonar-scanner` com relatório ESLint/TypeScript.
- Quality gate bloqueia merge em `main` se: novos bugs, vulnerabilities, code smells críticos/bloqueantes, ou cobertura de código novo abaixo do limiar definido no projeto Sonar.
- Nenhum secret ou token do Sonar é commitado; usar GitHub Actions secrets.

## Checklist de segurança (OWASP Top 10)

Aplicar em todo código novo, revisado no mesmo PR:

- validação de entrada em todos os endpoints (Bean Validation);
- nunca concatenar SQL (usar JPA/Query parametrizada);
- nunca logar secrets, tokens ou dados sensíveis de usuário;
- dependências desatualizadas/vulneráveis tratadas via Dependabot, não ignoradas;
- CORS e headers de segurança configurados explicitamente na API;
- uploads (screenshots) validados por tipo/tamanho antes de persistir.

---

# 77. Testes

## Unit

- returns;
- volatility;
- volume;
- events;
- score;
- risk;
- similarity.

## Integration

Testcontainers para PostgreSQL, migrations e persistência.

## E2E

```text
Market
 → API
 → Web
```

---

# 78. Testes de resiliência

Simular:

- Binance disconnect;
- Binance gap;
- PostgreSQL unavailable;
- Ollama unavailable;
- Ollama timeout;
- invalid JSON;
- duplicate candle;
- REST failure.

---

# 79. Performance

MVP deve suportar com folga:

```text
BTCUSDT
1m
1 candle/min
```

Não otimizar prematuramente para centenas de ativos.

---

# 80. Custo

Objetivo de desenvolvimento:

```text
custo operacional ≈ zero
```

Utilizar:

- Binance public data;
- PostgreSQL local;
- Docker;
- Ollama;
- processamento local.

---

# 81. Git

Branch principal:

```text
main
```

Features:

```text
feature/market-ingestion
feature/dashboard
feature/historical-analyzer
```

Experimentos:

```text
experiment/EXP-001
```

Commits pequenos e focados.

---

# 82. ADRs

Criar:

```text
ADR-001-java-25-lts.md
ADR-002-spring-boot.md
ADR-003-postgresql-18.md
ADR-004-modular-monolith.md
ADR-005-ollama-local.md
ADR-006-no-auto-trading.md
ADR-007-binance-market-data.md
ADR-008-multi-asset-readiness.md
ADR-009-no-webflux-mvp.md
ADR-010-postgresql-jsonb-hybrid.md
ADR-011-sonar-dependabot-quality-gate.md
ADR-012-execution-mode-isolation.md
ADR-013-feature-engine-no-external-lib.md
```

---

# 83. Roadmap

## Fase 0 — Walking Skeleton

- monorepo;
- Java 25;
- Spring Boot;
- PostgreSQL;
- Flyway;
- Next.js;
- Binance WebSocket;
- REST backfill;
- candles;
- dashboard live.

## Fase 1 — Features

- Feature Engine;
- persistence;
- versioning;
- tests.

## Fase 2 — Events

- Event Detector;
- Event model;
- versioning;
- tests.

## Fase 3 — Opportunity + Risk

- Opportunity Engine;
- score;
- Risk Engine;
- alert lifecycle;
- cooldown;
- deduplication.

## Fase 4 — Historical

- historical features;
- similarity;
- MFE;
- MAE;
- future returns;
- historical context.

## Fase 5 — Agent

- AgentProvider;
- Ollama;
- Agent Orchestrator;
- timeout;
- retry;
- structured output;
- graceful degradation.

## Fase 6 — Decision Memory

- decisions;
- screenshots;
- operations;
- OCR/Vision;
- P&L.

## Fase 7 — Research

- experiments;
- backtesting;
- OOS;
- walk-forward;
- regimes.

---

# 84. Definition of Done — produto inicial

O produto está pronto quando o usuário consegue:

```text
abrir cockpit
    ↓
ver BTC em tempo real
    ↓
receber evento/oportunidade
    ↓
entender por que recebeu
    ↓
ver contexto histórico
    ↓
ver risco
    ↓
ver interpretação da IA
    ↓
decidir
    ↓
registrar decisão
    ↓
registrar operação
    ↓
acompanhar resultado
```

---

# 85. Regra de parada para o coding agent

Se uma etapa falhar:

```text
STOP
 ↓
FIX
 ↓
TEST
 ↓
DOCUMENT
 ↓
CONTINUE
```

Não criar workaround silencioso.

---

# 86. Ordem exata de implementação

```text
1. Repository
2. Maven
3. Java 25
4. Spring Boot
5. Docker Compose
6. PostgreSQL
7. Flyway
8. Health checks
9. Market domain
10. Binance WebSocket
11. Candle persistence
12. Binance REST backfill
13. Gap detection
14. Gap recovery
15. Next.js
16. Real-time dashboard
17. Feature Engine
18. Event Detector
19. Opportunity Engine
20. Risk Engine
21. Historical Analyzer
22. Alert lifecycle
23. AgentProvider
24. Ollama
25. Agent Orchestrator
26. Decision Memory
27. Operations
28. Research/backtesting
```

Validar cada etapa antes de avançar.

---

# 87. Regras obrigatórias para o coding agent

1. Não implementar tudo de uma vez.
2. Começar pelo Walking Skeleton.
3. Não criar microserviços desnecessários.
4. Não adicionar Kafka/Kubernetes/Redis sem justificativa.
5. Não implementar trading automático.
6. Não utilizar LLM para matemática.
7. Não permitir lookahead.
8. Não alterar histórico silenciosamente.
9. Versionar engines/configurações.
10. Escrever testes junto com cada componente.
11. Usar `BigDecimal` para valores financeiros.
12. Usar `Instant` para timestamps.
13. Usar UTC internamente.
14. Não expor entidades JPA diretamente na API.
15. Não commitar secrets.
16. Não utilizar versões preview/beta/RC.
17. Manter o sistema funcional mesmo sem Ollama.
18. Preferir simplicidade à abstração prematura.
19. Nunca nomear classes de domínio com o nome do ativo (ex. `BitcoinCandle`) — usar `Instrument`/`symbol` como contexto, nunca como premissa arquitetural.
20. Diante de uma decisão não definida nesta spec, parar e apresentar explicitamente:

```text
DECISÃO PENDENTE
Contexto: ...
Opções: A / B / C
Recomendação: ...
Impacto: ...
```

Não inventar, não escolher por preferência pessoal, não aumentar escopo silenciosamente. A implementação só continua após a decisão ser validada pelo usuário.

---

# 88. Arquitetura final

```text
                         ┌─────────────────────┐
                         │       BINANCE       │
                         │    BTCUSDT / 1m     │
                         └──────────┬──────────┘
                                    │
                            WebSocket / REST
                                    │
                                    ▼
                 ┌──────────────────────────────────┐
                 │          SPRING BOOT              │
                 │        MODULAR MONOLITH           │
                 │                                  │
                 │  MARKET                           │
                 │    ↓                              │
                 │  FEATURE                          │
                 │    ↓                              │
                 │  EVENT                            │
                 │    ↓                              │
                 │  OPPORTUNITY                      │
                 │   ↙       ↘                       │
                 │ RISK    HISTORICAL                │
                 │   ↘       ↙                       │
                 │     AGENT                         │
                 └──────────────┬───────────────────┘
                                │
                                ▼
                           ┌─────────┐
                           │ OLLAMA  │
                           └────┬────┘
                                │
                                ▼
                           ┌─────────┐
                           │POSTGRES │
                           └────┬────┘
                                │
                                ▼
                           ┌─────────┐
                           │ NEXT.JS │
                           │ COCKPIT │
                           └────┬────┘
                                │
                                ▼
                             USUÁRIO
```

---

# 89. Princípio final

O sistema não deve tentar responder:

> "Como ganhar dinheiro com BTC?"

Deve responder:

> **"Existe agora uma situação de mercado suficientemente interessante, explicável, mensurável e historicamente sustentada para merecer minha atenção?"**

Fluxo:

```text
OBSERVAR
   ↓
MEDIR
   ↓
DETECTAR
   ↓
COMPARAR
   ↓
AVALIAR RISCO
   ↓
EXPLICAR
   ↓
ALERTAR
   ↓
REGISTRAR DECISÃO
   ↓
AVALIAR RESULTADO
   ↓
APRENDER
```

Sem execução automática.

Sem promessa de previsão.

Sem lookahead.

Sem depender de IA para matemática.

Sem transformar hipótese em verdade antes de testá-la.
