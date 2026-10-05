# Fase 6 — Decision Memory — SDD

**Depende de:** Fase 5 (Agent)
**Módulos:** `decision`, `operation`
**Referência:** [SDD.md](../../SDD.md) seções 52–57, 76

---

## 1. Objetivo

Permitir que o usuário registre decisões tomadas a partir dos alertas (BUY/SELL/IGNORE/WAIT), registre operações reais associadas (com screenshot opcional) e acompanhe P&L — construindo a memória pessoal de trading.

## 2. Escopo

**Dentro:**

- Registro manual de decisão do usuário por alerta
- Registro manual de operação (entrada/saída, quantidade, preço) associada a uma decisão
- Upload de screenshot associado à operação (sem OCR/Vision ainda — apenas armazenamento + confirmação manual)
- Cálculo de P&L (`grossPnl`, `fees`, `estimatedSlippage`, `netPnl`, `returnPct`, `holdingTime`)
- Separação clara entre fato do sistema (alerta), decisão do usuário e resultado de mercado (seção 57 do SDD)

**Fora:**

- OCR/Vision automático (fica para iteração futura, fora do roadmap de 8 fases)
- Integração autenticada com exchange

## 3. Modelo de dados

```sql
-- V9__create_user_decisions.sql
CREATE TABLE user_decisions (
    decision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id UUID NOT NULL REFERENCES alerts(alert_id),
    decision VARCHAR(10) NOT NULL, -- BUY/SELL/IGNORE/WAIT
    execution_mode VARCHAR(10) NOT NULL DEFAULT 'LIVE', -- LIVE / BACKTEST, herdado do alerta
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- V10__create_trades.sql
CREATE TABLE trades (
    trade_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    decision_id UUID NOT NULL REFERENCES user_decisions(decision_id),
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN', -- OPEN / CLOSED
    entry_operation_id UUID, -- FK adicionada após V11 (operations) via ALTER, ou referenciada por operations.trade_id
    exit_operation_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- V11__create_operations.sql
CREATE TABLE operations (
    operation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    decision_id UUID NOT NULL REFERENCES user_decisions(decision_id),
    trade_id UUID NOT NULL REFERENCES trades(trade_id),
    side VARCHAR(10) NOT NULL, -- BUY/SELL
    quantity NUMERIC(20,8) NOT NULL,
    price NUMERIC(20,8) NOT NULL,
    notional NUMERIC(24,8) NOT NULL,
    fees NUMERIC(20,8) NOT NULL DEFAULT 0,
    estimated_slippage NUMERIC(20,8) NOT NULL DEFAULT 0,
    occurred_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- V12__create_operation_screenshots.sql
CREATE TABLE operation_screenshots (
    screenshot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_id UUID NOT NULL REFERENCES operations(operation_id),
    file_path VARCHAR(500) NOT NULL,
    confidence VARCHAR(10), -- preenchido quando OCR existir no futuro
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Screenshots ficam fora do Git (`data/screenshots/`, incluído no `.gitignore`), com acesso controlado pela própria API (seção 76 do SDD).

## 4. Contratos / Interfaces

```java
public record RegisterDecisionRequest(UUID alertId, Decision decision) {}
public enum Decision { BUY, SELL, IGNORE, WAIT }

public record RegisterOperationRequest(
    UUID decisionId,
    Side side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal fees,
    BigDecimal estimatedSlippage,
    Instant occurredAt
) {}

public interface PnlCalculator {
    PnlResult calculate(Trade trade, Operation entry, Operation exit);
}

public record PnlResult(
    BigDecimal grossPnl,
    BigDecimal fees,
    BigDecimal estimatedSlippage,
    BigDecimal netPnl,
    BigDecimal returnPct,
    Duration holdingTime
) {}
```

`Trade` agrupa a operação de entrada e a de saída (SDD §53); o P&L pertence ao `Trade`, mesmo sendo calculado a partir das duas `Operation`.

## 5. Configuração

```yaml
operations:
  default-fee-rate: 0.001
  default-slippage-estimate: 0.0005
  screenshots:
    storage-path: /data/screenshots
```

## 6. Fluxo

```text
Alert exibido no cockpit
      ↓
POST /api/v1/decisions (BUY/SELL/IGNORE/WAIT)
      ↓
[opcional] POST /api/v1/operations (entrada)
      ↓
[opcional] POST /api/v1/operations/{id}/screenshot
      ↓
[futuro] POST /api/v1/operations (saída)
      ↓
PnlCalculator.calculate(entry, exit) → netPnl, returnPct, holdingTime
```

## 7. Critérios de aceite (DoD)

- Usuário consegue registrar decisão para qualquer alerta existente
- Operação pode ser registrada sem screenshot (screenshot é opcional nesta fase)
- P&L nunca considera apenas preço bruto — sempre descontando fees/slippage configuráveis
- Fato do sistema (classificação/score do alerta), decisão do usuário e resultado de mercado são consultáveis separadamente, nunca fundidos em um único registro mutável
- Operações de entrada/saída ficam agrupadas num `Trade`; o P&L nunca é calculado a partir de duas operações soltas sem vínculo explícito

## 8. Testes obrigatórios

- **Unit:** cálculo de P&L com fees/slippage variados, incluindo operação com prejuízo
- **Integration:** fluxo completo decisão → operação → screenshot → P&L
- **Validação:** operação não pode ser criada sem uma decisão associada válida

## 9. Observabilidade

Eventos: `decision_registered`, `operation_created`, `operation_updated`.

## 10. Riscos conhecidos

- Sem OCR nesta fase, a qualidade do dado de operação depende inteiramente da entrada manual do usuário — aceitável para o MVP, mas deixar o campo `source: MANUAL` explícito para diferenciar de uma futura extração automática.
