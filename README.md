# BTC Intelligence Cockpit

Sistema pessoal de inteligência de mercado para BTC/USDT — acompanha o mercado em tempo real, identifica eventos quantitativos, calcula contexto e risco e (em fases futuras) compara com situações históricas e usa IA local para interpretar evidências.

> Não executa ordens. Nenhuma decisão de trading é automatizada — a decisão final é sempre do usuário.

Arquitetura completa e decisões de design: [docs/SDD.md](docs/SDD.md). Specs por fase: [docs/phases/](docs/phases/).

**Fase atual implementada:** Fase 0 — Walking Skeleton (ingestão de mercado em tempo real + backfill histórico automático + dashboard mínimo).

## Stack

- **Backend:** Java 25 LTS, Spring Boot 4.x (servlet stack, sem WebFlux), PostgreSQL 18, Flyway.
- **Frontend:** Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4.
- **Dados de mercado:** Binance (WebSocket para candles ao vivo + REST para backfill histórico).

## Pré-requisitos

| Ferramenta | Versão | Necessário para |
|---|---|---|
| Java (JDK) | 25 LTS | rodar/compilar o backend localmente (fora do Docker) |
| Maven | 3.9+ | build do backend |
| Node.js | 24 LTS | rodar o frontend localmente (fora do Docker) |
| pnpm | 9+ | gerenciar dependências do frontend |
| Docker + Docker Compose | recentes | subir a stack completa (postgres + api + web) |

Para rodar via `docker compose`, só Docker é necessário — Java/Maven/Node/pnpm só são precisos para desenvolvimento local fora de containers.

## Subindo a stack completa (recomendado)

```bash
cp .env.example .env   # ajuste se necessário
scripts/up.sh
```

Isso builda e sobe os 3 serviços: `postgres` (5432), `api` (8080), `web` (3000). Abra [http://localhost:3000](http://localhost:3000).

Para validar rapidamente sem esperar o backfill padrão de 12 meses:

```bash
scripts/up.sh 2-days
```

Outros scripts úteis em `scripts/`:

| Script | O que faz |
|---|---|
| `build.sh` | Compila o backend (`mvn compile`) |
| `test.sh` | Roda a suíte completa de testes (unit + integration com Testcontainers; precisa do Docker rodando) |
| `up.sh [PERIODO]` | `docker compose up --build -d`, com override opcional de `BACKFILL_PERIOD` |
| `down.sh [-v]` | Derruba a stack (`-v` também remove o volume do Postgres) |
| `logs.sh` | Acompanha os logs da API em tempo real |
| `status.sh [SYMBOL]` | Consulta `GET /api/v1/market/{symbol}/status` (default `BTCUSDT`) |
| `all.sh [PERIODO]` | Pipeline completo: compila, testa, sobe a stack e aguarda ficar healthy |

## Rodando localmente sem Docker (desenvolvimento)

### Backend

```bash
cd backend
JAVA_HOME=~/.sdkman/candidates/java/25.0.4-tem mvn spring-boot:run
```

Requer um Postgres acessível (ex.: `docker compose up postgres -d`) e as variáveis de ambiente de `.env.example` exportadas (ou defaults locais: `DB_HOST=localhost`, `DB_USER=btc_assistant`, etc.).

### Frontend

```bash
cd frontend
cp .env.local.example .env.local
pnpm install
pnpm dev
```

Abre em [http://localhost:3000](http://localhost:3000), apontando para a API em `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`).

## Testes

```bash
scripts/test.sh
```

Roda testes unitários e de integração (Testcontainers sobe um Postgres real para validar migrations, constraints e o fluxo de backfill — Docker precisa estar rodando). Para rodar um teste específico: `scripts/test.sh -Dtest=NomeDaClasse`.

Frontend:

```bash
cd frontend
pnpm lint
pnpm build
```

## Backfill histórico automático

Ao subir, cada instrumento configurado (`market.instruments` em `application.yml`, ex. `BTCUSDT`) passa por uma máquina de estados antes de ficar disponível para uso ao vivo:

```text
INITIALIZING → CHECKING_HISTORY → BACKFILLING → (RETRYING se falhar) → HISTORICAL_DATA_READY → LIVE
```

- **Idempotente:** reiniciar a aplicação não reprocessa candles já persistidos — o sistema verifica a cobertura existente (`HistoricalCoverageChecker`) e busca apenas o que falta.
- **Período configurável:** `BACKFILL_PERIOD` (default `12-months`; aceita formatos como `2-days` para testes rápidos).
- **Recuperação de gaps:** se o WebSocket cair e reconectar, o sistema detecta e recupera o intervalo perdido automaticamente (gap recovery), sem intervenção manual.
- **Consulta de status:** `GET /api/v1/market/{symbol}/status` expõe a fase atual, se o WebSocket está conectado, se o histórico está pronto, contagem de candles e o range coberto.
- **Degradação graciosa:** a ingestão ao vivo via WebSocket funciona independentemente do estado do backfill — um backfill em andamento (ou com falha temporária, até `BACKFILL_MAX_RETRIES`) não bloqueia o recebimento de candles novos.

## Endpoints principais (Fase 0)

- `GET /api/v1/market/status` — status de todos os instrumentos configurados.
- `GET /api/v1/market/{symbol}/status` — status de um instrumento.
- `GET /api/v1/market/{symbol}/latest` — último candle fechado.
- `GET /api/v1/market/candles?symbol=...&from=...&to=...` — candles em um intervalo.
- `GET /actuator/health` — health check (usado pelo `all.sh` para aguardar a API subir).
- WebSocket STOMP em `/ws` (SockJS): tópicos `/topic/market/{symbol}` (candles ao vivo) e `/topic/market-status/{symbol}` (mudanças de status).

## Regras não-negociáveis do projeto

Ver [.github/copilot-instructions.md](.github/copilot-instructions.md) para a lista completa (sem lookahead, imutabilidade de dados de mercado/oportunidades/alertas, sem auto-trading, LLM nunca calcula métricas, precisão financeira com `BigDecimal`/`Instant`, degradação graciosa do pipeline quantitativo).
