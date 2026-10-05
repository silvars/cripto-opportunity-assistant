# Fase 1 — Feature Engine — SDD

**Depende de:** Fase 0 (Walking Skeleton)
**Módulos:** `feature`
**Referência:** [SDD.md](../../SDD.md) seções 18, 19

---

## 1. Objetivo

Transformar candles persistidos em indicadores quantitativos explicáveis (retornos, volume, volatilidade, extremos, momentum, estrutura), calculados deterministicamente — sem envolver o LLM.

## 2. Escopo

**Dentro:**

- Cálculo incremental de features a cada novo candle 1m persistido
- Features: `return_*`, `volume_sma/ratio/zscore`, `volatility_*`, `distance_recent_high/low_*`, `momentum_*`, `drawdown_from_recent_high`, `recovery_from_recent_low`
- Versionamento (`featureVersion`)
- Persistência das features calculadas
- Exposição via API (`GET /api/v1/features/latest`)

**Fora:**

- Event Detector, Opportunity/Risk Engine, Historical Analyzer, Agent

## 3. Modelo de dados

```sql
-- V2__create_market_features.sql
CREATE TABLE market_features (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    interval VARCHAR(10) NOT NULL,
    candle_open_time TIMESTAMPTZ NOT NULL,
    feature_version VARCHAR(10) NOT NULL,
    return_1m NUMERIC(10,6), return_5m NUMERIC(10,6), return_15m NUMERIC(10,6),
    return_30m NUMERIC(10,6), return_1h NUMERIC(10,6), return_4h NUMERIC(10,6),
    return_12h NUMERIC(10,6), return_24h NUMERIC(10,6),
    volume_sma NUMERIC(24,8), volume_ratio NUMERIC(10,4), volume_zscore NUMERIC(10,4),
    volatility_15m NUMERIC(10,6), volatility_1h NUMERIC(10,6),
    volatility_4h NUMERIC(10,6), volatility_24h NUMERIC(10,6),
    distance_recent_high_1h NUMERIC(10,6), distance_recent_high_4h NUMERIC(10,6), distance_recent_high_24h NUMERIC(10,6),
    distance_recent_low_1h NUMERIC(10,6), distance_recent_low_4h NUMERIC(10,6), distance_recent_low_24h NUMERIC(10,6),
    momentum_5m NUMERIC(10,6), momentum_15m NUMERIC(10,6), momentum_1h NUMERIC(10,6), momentum_4h NUMERIC(10,6),
    drawdown_from_recent_high NUMERIC(10,6), recovery_from_recent_low NUMERIC(10,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_feature UNIQUE (symbol, interval, candle_open_time, feature_version)
);
```

Mudança de fórmula → novo `feature_version`; linhas antigas nunca são sobrescritas com novo significado (seção 19 do SDD).

## 4. Contratos / Interfaces

Sem dependência de bibliotecas de dataframe/numéricas externas (Tablesaw/Smile) — decisão encerrada: implementação própria em Java, isolada por componente e testável:

```java
public interface ReturnCalculator { BigDecimal calculate(RollingWindow window, Duration horizon); }
public interface VolatilityCalculator { BigDecimal calculate(RollingWindow window, Duration horizon); }
public interface VolumeAnalyzer { VolumeStats analyze(RollingWindow window); }
public interface MomentumCalculator { BigDecimal calculate(RollingWindow window, Duration horizon); }
public interface ZScoreCalculator { BigDecimal zscore(BigDecimal value, RollingWindow window); }

public final class RollingWindow {
    // janela de candles em memória/consulta, sobre BigDecimal — sem array numérico externo
}

public interface FeatureCalculator {
    String featureVersion();
    FeatureSnapshot calculate(Instrument instrument, Instant atCandleOpenTime);
}

public record FeatureSnapshot(
    String symbol,
    String interval,
    Instant candleOpenTime,
    String featureVersion,
    Map<String, BigDecimal> values
) {}
```

Cada grupo de feature (returns, volume, volatility, extremes, momentum, structure) é um componente isolado e testável, combinado por um `FeatureEngine` orquestrador.

## 5. Configuração

```yaml
feature-engine:
  version: "1.0"
  windows:
    volume-sma: 20
    volatility-lookback: 96 # candles de 15m equivalentes
```

## 6. Fluxo

```text
candle_persisted (evento interno)
      ↓
FeatureEngine.onCandle()
      ↓
calcula janelas (1m..24h) a partir do histórico já persistido
      ↓
MarketFeatureRepository (upsert por feature_version)
      ↓
feature_update (evento interno/WebSocket)
```

**No-lookahead:** cálculo de features em `T` usa apenas candles com `open_time <= T`.

## 7. Critérios de aceite (DoD)

- Toda feature listada na seção 18 do SDD está implementada e testada
- Features são recalculadas apenas para novos candles (não reprocessa histórico silenciosamente)
- `feature_version` é gravado em cada linha
- API retorna o snapshot de features mais recente
- Nenhuma feature usa dado posterior ao candle de referência (teste automatizado)

## 8. Testes obrigatórios

- **Unit:** cada fórmula testada com dataset sintético e valor esperado conhecido (golden values)
- **Determinismo:** mesmo candle + histórico → mesmo resultado sempre
- **No-lookahead:** calcular features em `T`, adicionar candles futuros, recalcular em `T` → resultado idêntico
- **Integration:** persistência e upsert por `feature_version`

## 9. Observabilidade

Novo evento de log: `feature_calculated`, `feature_calculation_failed`.

## 10. Decisões pendentes

Nenhuma — biblioteca numérica decidida (implementação própria, ver seção 4).
