# Fase 3 — Opportunity + Risk — SDD

**Depende de:** Fase 2 (Event Detector)
**Módulos:** `opportunity`, `risk`, `alert`
**Referência:** [SDD.md](../SDD.md) seções 22–26, 41–44

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

## 11. Decisões de implementação (onde a especificação original era ambígua)

- **Orquestração em módulo próprio (`pipeline`)**: o fio condutor evento→oportunidade→risco→alerta não vive dentro de `opportunity` nem `alert` — `alert` já depende de `opportunity` (FK `opportunity_id` + cópia de `classification`/`direction`/`risk_level`); se o orquestrador vivesse em `opportunity` e chamasse `AlertService`, criaria uma dependência circular entre os dois módulos. Vive em `com.btcassistant.pipeline.OpportunityPipeline`.
- **Gatilho**: `EventPersister.persist(...)` passou a retornar o `eventId` gerado; `EventDetector` publica `MarketEventPersistedEvent(eventId, DetectedEvent)` (análogo a `FeatureCalculatedEvent`/`CandlePersistedEvent` das fases anteriores) — carrega o `DetectedEvent` completo (já inclui o mapa de features e o preço), evitando uma segunda consulta ao banco.
- **`OpportunityResult.engineVersion`**: campo adicionado ao rascunho original do SDD — necessário para popular a coluna `engine_version` de `opportunities` (`NOT NULL` no schema), que o contrato original do SDD não cobria. Versão fixa `"1.0"` do algoritmo de combinação de componentes, distinta de `configVersion` (pesos/thresholds).
- **`scoreComponents` sem marcador "UNKNOWN" inline**: o exemplo do SDD §24 usa um objeto JSON com `"historicalSupport": "UNKNOWN"` (string) misturado com números, mas o contrato Java tipa `scoreComponents` como `Map<String,BigDecimal>` — incompatível. Decisão: componentes UNKNOWN são **omitidos** do mapa (nunca aparecem como chave), e `availableEvidenceWeight`/`evidenceQuality` já comunicam a ausência de forma estruturada.
- **`availableEvidenceWeight` no exemplo do SDD §24 não bate com os pesos do próprio SDD**: os pesos configurados somam 110 (20+30+15+15+10+20), não 100, mas o exemplo afirma `availableEvidenceWeight = 0.80` (que só seria exato se o total fosse 100). A implementação calcula `availableWeight/totalWeight` usando o total real configurado (110 por padrão) — matematicamente correto e não assume silenciosamente que os pesos somam 100. O **score** em si (64/80×100=80) não é afetado por essa inconsistência e bate exatamente com o exemplo do SDD (ver `OpportunityEngineImplTest`).
- **`evidenceQuality` é sempre `LIMITED` nesta fase**: como `historicalSupport` nunca resolve (placeholder até a Fase 4), `evidenceQuality = FULL` é estruturalmente inalcançável até lá — comportamento intencional, testado explicitamente.
- **Fórmulas dos 6 componentes do score e do Risk Engine**: o SDD lista os nomes dos componentes/inputs mas não dá fórmulas. Decisões (todas config-driven, nunca hardcoded):
  - `eventStrength`: normaliza a magnitude do evento por "família" (choque percentual, proximidade — invertido, z-score de volume, razão de volatilidade) contra uma escala própria configurável; único componente nunca UNKNOWN (todo evento tem magnitude).
  - `momentumContext`: `return_15m` na direção do evento (ou valor absoluto para eventos `NEUTRAL`) sobre uma escala configurável.
  - `volumeConfirmation`: `volume_zscore` sobre uma escala configurável.
  - `volatilityContext`: razão `volatility_15m/volatility_4h` sobre uma escala configurável.
  - `riskPenalty`: `1 - clamp(volatility_24h/escala, 0, 1)` — quanto maior a volatilidade de 24h, menor a contribuição.
  - `RiskEngine`: composição ponderada (pesos configuráveis) de volatilidade de 24h, proximidade de extremos de 1h e magnitude do evento, mapeada para LOW/MEDIUM/HIGH por thresholds configuráveis; `UNKNOWN` apenas quando `volatility_24h` está ausente (sinal mais importante).
- **Direção implícita do evento** (`EventDirectionMapper`): eventos direcionais (`RAPID_RISE`/`RECOVERY`/`BREAKOUT` → BUY; `RAPID_DROP`/`BREAKDOWN`/`FAILED_RECOVERY`/`FAILED_BREAKOUT` → SELL) mapeiam diretamente; eventos de contexto/volatilidade (`VOLUME_SPIKE`, `VOLATILITY_*`, `LOCAL_*_APPROACH`) não implicam direção por si só → `NEUTRAL`.
- **Alert lifecycle colapsado em uma única escrita**: `DETECTED→ANALYZING→CREATED→ACTIVE` são percorridos em código (cada `transitionTo` é chamado, logável) mas só a linha final (`ACTIVE`) é persistida — evita 4 INSERTs/UPDATEs por alerta novo. `EVALUATED` não é alcançado nesta fase (depende de mecanismos de avaliação de outcome da Fase 6).
- **"Atualiza o alerta ativo" (DoD) não significa UPDATE de campos**: o SDD §42 afirma explicitamente "apenas a coluna status muda" em `alerts` — logo, dedup/cooldown nunca mutam score/preço/etc. de um alerta existente. "Atualizar" significa: **suprimir a criação de um novo alerta** enquanto um já está `ACTIVE` (ou foi criado há menos que `dedup-window`) para o mesmo símbolo+direção. Cada evento, mesmo suprimido, já gerou sua própria linha imutável em `opportunities` — nada se perde para auditoria.
- **Expiração preguiçosa**: `AlertService` verifica e transiciona alertas vencidos (`ACTIVE`→`EXPIRED`, a única mutação permitida) no início do processamento do próximo evento candidato, em vez de um `@Scheduled` em background — suficiente para esta fase, sem a complexidade de agendamento.
- **`market_snapshot`**: simplificado para `{"price": <preço no momento do evento>}` — o SDD não detalha o conteúdo esperado; evita buscar o candle completo de novo (preço já está disponível via `DetectedEvent.price()`).
