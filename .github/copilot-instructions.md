# BTC Opportunity Assistant — Project Guidelines

This repo follows [SDD.md](../SDD.md) (architecture) and the per-phase design docs in [docs/phases/](../docs/phases/). Read the relevant phase doc before implementing anything in that phase.

## Architecture

- Java 25 LTS / Spring Boot 4.x modular monolith (`backend/`, package `com.btcassistant.<module>`) + Next.js 16 (`frontend/`). See SDD.md §4–6.
- Modules are domain boundaries, not microservices: `market`, `feature`, `event`, `opportunity`, `risk`, `historical`, `agent`, `alert`, `decision`, `operation`, `research`.
- No Spring WebFlux (ADR-009) — servlet stack only, blocking JPA. Never mix reactive types into this codebase.
- `symbol`/instrument is always a config dimension, never hardcoded (ADR-008). Don't special-case "BTC" in business logic.
- PostgreSQL only, no separate NoSQL store (ADR-010). Stable/filterable fields are typed columns; volatile/evolving fields (feature snapshots, score breakdown, agent output) are `JSONB`.

## Non-negotiable rules

- **No lookahead, ever.** A calculation at time `T` may only use data with `timestamp <= T`. This applies to features, events, opportunities, scores, historical similarity, and the agent. Every new engine/feature/historical logic needs an automated lookahead regression test (SDD.md §60).
- **Immutability.** `market_events`, `opportunities`, and `alerts` core fields are insert-only — never add `UPDATE` methods for them. New logic versions (`engineVersion`/`featureVersion`/`configVersion`/`historicalVersion`/`agentVersion`) create new rows; old rows are never reinterpreted.
- **No auto-trading.** Never implement order execution, exchange write/trade credentials, or withdrawal capability.
- **LLM never computes metrics.** Ollama/agent code only interprets data already computed deterministically. Missing data is serialized as `"status": "not_available"`, never guessed.
- **Financial precision.** Use `BigDecimal` for price/volume/P&L, `Instant` for timestamps, `Duration` for durations. Never `double`/`float` for money.
- **Graceful degradation.** The quantitative pipeline (market → features → events → opportunity → risk) must keep working if Ollama or the historical analyzer is unavailable.

## Code Style

- Java: no preview/beta/RC/SNAPSHOT dependencies. DTOs/API responses as `record`s; never expose JPA entities directly on the API.
- Flyway migrations are append-only (`V{n}__description.sql`); never edit an already-applied migration.
- Config-driven thresholds/weights (YAML), never hardcoded magic numbers in business logic.

## Build and Test

- Backend: `cd backend && mvn verify` (runs tests + Checkstyle/PMD/SpotBugs if configured).
- Frontend: `cd frontend && pnpm install && pnpm build && pnpm lint`.
- Full stack locally: `docker compose up --build`.
- Integration tests use Testcontainers (real PostgreSQL), not mocks, for persistence/migration tests.

## Security & Quality Gates (SDD.md §76)

- Dependabot is enabled for Maven, npm, Docker, and GitHub Actions ([.github/dependabot.yml](./dependabot.yml)) — don't ignore or defer vulnerability PRs.
- SonarCloud/SonarQube quality gate must pass before merge (new bugs/vulnerabilities/critical smells block the PR; coverage on new code must meet the project threshold).
- Never commit secrets; `.env` only, always in `.gitignore`. Validate all inputs (Bean Validation), use parameterized queries only, don't log secrets or sensitive user data.
- Follow OWASP Top 10 checklist in SDD.md §76 for any new endpoint or external integration.
