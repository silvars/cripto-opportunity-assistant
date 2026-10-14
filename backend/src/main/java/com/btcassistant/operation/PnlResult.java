package com.btcassistant.operation;

import java.math.BigDecimal;
import java.time.Duration;

/** Resultado do P&L de um {@link TradeEntity} fechado (SDD §55). */
public record PnlResult(
    BigDecimal grossPnl,
    BigDecimal fees,
    BigDecimal estimatedSlippage,
    BigDecimal netPnl,
    BigDecimal returnPct,
    Duration holdingTime
) {
}
