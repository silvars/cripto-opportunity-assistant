package com.btcassistant.operation;

/** P&L nunca considera apenas preço bruto — sempre descontando fees/slippage (SDD §55, DoD Fase 6). */
public interface PnlCalculator {

    PnlResult calculate(TradeEntity trade, OperationEntity entry, OperationEntity exit);
}
