# Fase 3 — Opportunity + Risk — SDD

**Depende de:** Fase 2 (Event Detector)
**Módulos:** `opportunity`, `risk`, `alert`
**Referência:** [SDD.md](../../SDD.md) seções 22–26, 41–44

---

## 1. Objetivo

Transformar eventos + contexto quantitativo em uma classificação de oportunidade explicável, com avaliação de risco e ciclo de vida de alerta (sem IA ainda).

## 2. Escopo

**Dentro:**

- Opportunity Engine: classificação (`NONE/WATCH/SETUP/STRONG_SETUP`), direção (`BUY/SELL/NEUTRAL`), score 0–100 explicável por componentes
- Risk Engine: `LOW/MEDIUM/HIGH/UNKNOWN` com razões
- Alert lifecycle: `DETECTED → ANALYZING → CREATED → ACTIVE → EXPIRED → EVALUATED`
- Cooldown + deduplicação + agrupamento de eventos semelhantes
- Configuração de pesos/thresholds versionada (`configVersion`)

**Fora:**

- Historical Analyzer (o componente `historicalSupport` do score fica como `UNKNOWN`/placeholder até a Fase 4 existir)
- Agent/Ollama

> Nota: a Fase 4 (Historical Analyzer) alimenta o componente `historicalSupport` do score. Nesta fase ele retorna `UNKNOWN` sem travar o pipeline — graceful degradation aplicado desde já (ver regra de renormalização na seção 4).

## 3. Modelo de dados

```sql
-- V4__create_opportunities.sql
CREATE TABLE opportunities (
    opportunity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES market_events(event_id),
    symbol VARCHAR(20) NOT NULL,
    classification VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    score NUMERIC(5,2) NOT NULL,
    score_components JSONB NOT NULL,
    available_evidence_weight NUMERIC(4,3) NOT NULL, -- ex. 0.80 = 80% do peso configurado
    evidence_quality VARCHAR(10) NOT NULL, -- FULL / LIMITED
    risk_level VARCHAR(10) NOT NULL,
    risk_reasons JSONB NOT NULL,
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'LIVE', -- LIVE / BACKTEST
    config_version VARCHAR(10) NOT NULL,
    engine_version VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- V5__create_alerts.sql
CREATE TABLE alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(opportunity_id),
    symbol VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    score NUMERIC(5,2) NOT NULL,
    classification VARCHAR(20) NOT NULL,
    risk_level VARCHAR(10) NOT NULL,
    price_at_alert NUMERIC(20,8) NOT NULL,
    market_snapshot JSONB NOT NULL,
    feature_snapshot JSONB NOT NULL,
    engine_version VARCHAR(10) NOT NULL,
    config_version VARCHAR(10) NOT NULL,
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'LIVE', -- LIVE / BACKTEST
    status VARCHAR(20) NOT NULL DEFAULT 'DETECTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ
);
```

`opportunities` é imutável (uma linha por avaliação). Em `alerts`, apenas a coluna `status` muda ao longo do lifecycle — todos os demais campos são fixados na criação e nunca alterados (seção 34 do SDD anterior / princípio de auditabilidade, seção 2.4 do SDD atual). O dashboard operacional só exibe `execution_mode = LIVE` por padrão (SDD §59).

## 4. Contratos / Interfaces

```java
public interface OpportunityEngine {
    OpportunityResult evaluate(DetectedEvent event, FeatureSnapshot features, Optional<HistoricalContext> historicalContext);
}

public record OpportunityResult(
    Direction direction,
    Classification classification,
    BigDecimal score,
    Map<String, BigDecimal> scoreComponents,
    BigDecimal availableEvidenceWeight, // 0.0–1.0, % do peso total configurado usado no cálculo
    EvidenceQuality evidenceQuality,    // FULL / LIMITED
    String configVersion
) {}

public enum EvidenceQuality { FULL, LIMITED }

public interface RiskEngine {
    RiskAssessment assess(DetectedEvent event, FeatureSnapshot features);
}

public record RiskAssessment(RiskLevel level, List<String> reasons) {}

public enum RiskLevel { LOW, MEDIUM, HIGH, UNKNOWN }
public enum Classification { NONE, WATCH, SETUP, STRONG_SETUP }
public enum Direction { BUY, SELL, NEUTRAL }
```

**Regra de renormalização (SDD §24):** quando um componente do score está `UNKNOWN` (ex. `historicalSupport` antes da Fase 4 existir), ele não entra no numerador; o peso dos componentes disponíveis é renormalizado sobre o peso total disponível (`score = contribuição_disponível / peso_disponível × 100`). A classificação (`NONE/WATCH/SETUP/STRONG_SETUP`) continua decidida apenas pelos thresholds — **não existe regra implícita que limite a classificação a `WATCH` só por um componente estar ausente**. `evidenceQuality` cai para `LIMITED` sempre que houver componente crítico `UNKNOWN`.

## 5. Configuração

```yaml
opportunity:
  watch-threshold: 50
  setup-threshold: 65
  strong-setup-threshold: 80
  scoring:
    event-strength: 20
    historical-support: 30
    momentum-context: 15
    volume-confirmation: 15
    volatility-context: 10
    risk-penalty: 20
  config-version: "1.0"

alerts:
  cooldown: 15m
  dedup-window: 15m
```

## 6. Fluxo

```text
event_detected
      ↓
OpportunityEngine.evaluate()  (historicalContext = UNKNOWN nesta fase)
      ↓
RiskEngine.assess()
      ↓
score >= watch-threshold?
   não → status NONE, não gera alerta
   sim → verifica cooldown/dedup
      ↓
AlertService.createOrUpdate()
      ↓
alert_created / alert_updated (WebSocket)
```

## 7. Critérios de aceite (DoD)

- `NONE` é tratado como resultado normal, sem forçar geração de alerta
- Score é decomponível (cada componente auditável via API)
- Thresholds e pesos alteráveis via YAML sem mudança de código
- Durante um movimento prolongado, não gera alertas duplicados — atualiza o alerta ativo
- Risk Engine retorna `UNKNOWN` quando dados insuficientes, nunca "inventa" confiança
- Toda oportunidade/alerta registra `engineVersion`, `configVersion` e `executionMode`
- Componente `UNKNOWN` nunca vira `0`/neutro; score é renormalizado sobre o peso disponível (ver seção 4) e `availableEvidenceWeight`/`evidenceQuality` ficam explícitos na resposta

## 8. Testes obrigatórios

- **Unit:** cálculo de score com componentes conhecidos; thresholds de classificação; Risk Engine com dados insuficientes → `UNKNOWN`
- **Unit (renormalização):** com um componente `UNKNOWN`, score recalculado sobre peso disponível bate com o exemplo do SDD §24 (peso 80/100 → score 64/80×100); `evidenceQuality = LIMITED`; classificação não é artificialmente capada em `WATCH`
- **Integration:** pipeline evento → oportunidade → risco → alerta persistido
- **Cooldown/dedup:** sequência de eventos semelhantes em curto intervalo gera um único alerta ativo
- **Config versioning:** mudar `config_version` não altera registros antigos

## 9. Observabilidade

Eventos: `opportunity_created`, `alert_created`, `alert_updated`, `alert_expired`.

## 10. Riscos conhecidos

- Score com `historicalSupport = UNKNOWN` até a Fase 4 existir é renormalizado e marcado como `evidenceQuality = LIMITED` — documentar isso no dashboard ("contexto histórico indisponível") em vez de ocultar silenciosamente.
