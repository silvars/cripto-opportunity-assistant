package com.btcassistant.research;

/** Processa candles em ordem cronológica estrita — nunca calcula resultado futuro antes de registrar
 * o sinal simulado (SDD §59). */
public interface BacktestRunner {

    BacktestResult run(BacktestConfig config);
}
