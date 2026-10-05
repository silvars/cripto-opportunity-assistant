# Fase 7 — Research / Learning — SDD

**Depende de:** Fase 6 (Decision Memory)
**Módulos:** `research` (+ uso transversal de `historical`, `opportunity`, `alert`, `decision`)
**Referência:** [SDD.md](../../SDD.md) seções 58–61, 64–68, 83

---

## 1. Objetivo

Formalizar a experimentação: avaliar alertas e decisões ignoradas, rodar backtests reprodutíveis, e produzir evidência estatística (com holdout/OOS) sobre quais setups realmente funcionam — sem transformar hipótese em regra antes de validá-la.

## 2. Escopo

**Dentro:**

- Avaliação de alertas: resultado futuro por horizonte (`future_5m` ... `future_24h`, `MFE`, `MAE`, `targetBeforeStop`)
- Avaliação de decisões ignoradas ("missed opportunities")
- Backtesting cronológico determinístico, reprodutível
- Estrutura de experimentos (`research/experiments/EXP-XXX/`) com README, config, resultados e conclusão
- Suporte a holdout / walk-forward / out-of-sample
- Métricas de qualidade do assistente (precision de alertas fortes, falso positivo, cobertura, MFE/MAE médios, performance por setup/regime)

**Fora:**

- Classificação de regime (`UPTREND/DOWNTREND/SIDEWAYS/...`) completa — pode ficar como próximo incremento, não bloqueante para o fechamento desta fase
- Qualquer forma de otimização de parâmetros usando dados out-of-sample (proibido pelo SDD, seção 3/67)

## 3. Modelo de dados

```sql
-- V13__create_experiment_runs.sql
CREATE TABLE experiment_runs (
    experiment_run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_code VARCHAR(20) NOT NULL, -- 'EXP-001'
    engine_version VARCHAR(10) NOT NULL,
    feature_version VARCHAR(10) NOT NULL,
    config_version VARCHAR(10) NOT NULL,
    historical_version VARCHAR(10) NOT NULL,
    target_definition_version VARCHAR(10) NOT NULL,
    stop_definition_version VARCHAR(10) NOT NULL,
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'BACKTEST',
    dataset_period TSRANGE NOT NULL,
    is_out_of_sample BOOLEAN NOT NULL DEFAULT false,
    results JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- V14__create_alert_evaluations.sql
CREATE TABLE alert_evaluations (
    alert_id UUID PRIMARY KEY REFERENCES alerts(alert_id),
    future_5m NUMERIC(10,6), future_15m NUMERIC(10,6), future_30m NUMERIC(10,6),
    future_1h NUMERIC(10,6), future_4h NUMERIC(10,6), future_12h NUMERIC(10,6), future_24h NUMERIC(10,6),
    mfe NUMERIC(10,6), mae NUMERIC(10,6),
    target_before_stop BOOLEAN,
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'LIVE',
    evaluated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

`alert_evaluations` só pode ser preenchida depois que o horizonte correspondente já decorreu de fato (nunca antecipadamente). `execution_mode` garante que avaliações de `BACKTEST` nunca se misturem com avaliações de alertas `LIVE` em consultas operacionais (SDD §59).

## 4. Contratos / Interfaces

```java
public interface BacktestRunner {
    BacktestResult run(BacktestConfig config);
}

public record BacktestConfig(
    String symbol,
    Instant from,
    Instant to,
    String engineVersion,
    String featureVersion,
    String configVersion,
    String historicalVersion,
    String targetDefinitionVersion,
    String stopDefinitionVersion,
    boolean outOfSample
) {}

public interface AlertEvaluator {
    void evaluatePendingAlerts(Instant asOf);
}
```

`BacktestRunner` processa candles em ordem cronológica estrita, nunca calculando resultado futuro antes de registrar a decisão simulada (seção 22/59 do SDD anterior).

## 5. Configuração

```yaml
research:
  experiments-path: research/experiments
  backtest:
    chunk-size: 30d
  evaluation:
    horizons: [5m, 15m, 30m, 1h, 4h, 12h, 24h]
```

## 6. Fluxo

```text
EXP-XXX/config.yaml
      ↓
BacktestRunner.run() — processa candles em ordem cronológica
      ↓
para cada decisão simulada: registra opportunity/alert como se estivesse em tempo real
      ↓
[somente depois] AlertEvaluator.evaluatePendingAlerts() preenche resultado futuro
      ↓
results.json + conclusion.md em research/experiments/EXP-XXX/
```

## 7. Critérios de aceite (DoD)

- EXP-001 (Rapid Drop) executado e documentado seguindo o template da seção 41 do SDD anterior (hypothesis/dataset/methodology/results/limitations/conclusion)
- Rodar o mesmo backtest duas vezes com mesmas versões (`engineVersion`, `featureVersion`, `configVersion`, `historicalVersion`, `targetDefinitionVersion`, `stopDefinitionVersion`) produz resultado idêntico
- Avaliação de alertas ignorados ("missed opportunities") disponível
- Existe ao menos um experimento com separação holdout/out-of-sample, não apenas in-sample
- Nenhum parâmetro foi ajustado olhando para o conjunto out-of-sample

## 8. Testes obrigatórios

- **Reprodutibilidade:** mesmo backtest, mesmas versões → mesmo resultado byte-a-byte
- **No-lookahead (backtest):** mesmo teste da Fase 4, agora aplicado end-to-end sobre o backtest completo
- **Unit:** cálculo de métricas de avaliação (MFE/MAE/targetBeforeStop) com casos sintéticos conhecidos

## 9. Observabilidade

Eventos: `backtest_started`, `backtest_completed`, `alert_evaluated`.

## 10. Riscos conhecidos

- Tentação natural de ajustar thresholds olhando resultados do próprio conjunto de teste — processo de experimento (seção 3) deve exigir declarar hipótese e critério **antes** de rodar, para evitar overfitting de parâmetros.
