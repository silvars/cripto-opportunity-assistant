package com.btcassistant.opportunity;

/** Identifica se o registro veio do pipeline em tempo real ou de uma simulação de backtest (SDD §42/§59). */
public enum ExecutionMode {
    LIVE,
    BACKTEST
}
