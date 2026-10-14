package com.btcassistant.operation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;

/** Long = entrada BUY/saída SELL; short = entrada SELL/saída BUY (SDD §55). */
@Component
public class SimplePnlCalculator implements PnlCalculator {

    @Override
    public PnlResult calculate(TradeEntity trade, OperationEntity entry, OperationEntity exit) {
        BigDecimal priceDelta = entry.getSide() == Side.BUY
                ? exit.getPrice().subtract(entry.getPrice())
                : entry.getPrice().subtract(exit.getPrice());
        BigDecimal grossPnl = priceDelta.multiply(entry.getQuantity());

        BigDecimal fees = entry.getFees().add(exit.getFees());
        BigDecimal estimatedSlippage = entry.getEstimatedSlippage().add(exit.getEstimatedSlippage());
        BigDecimal netPnl = grossPnl.subtract(fees).subtract(estimatedSlippage);

        BigDecimal entryNotional = entry.getPrice().multiply(entry.getQuantity());
        BigDecimal returnPct = entryNotional.signum() == 0
                ? BigDecimal.ZERO
                : netPnl.divide(entryNotional, MathContext.DECIMAL64);

        Duration holdingTime = Duration.between(entry.getOccurredAt(), exit.getOccurredAt());

        return new PnlResult(grossPnl, fees, estimatedSlippage, netPnl, returnPct, holdingTime);
    }
}
