package com.btcassistant.agent;

/**
 * O agente recebe o instrumento como dado de entrada, nunca como premissa fixa de código ou de
 * prompt (SDD §36/§37 — "nunca assume Bitcoin no prompt ou no código").
 */
public record InstrumentContext(String symbol, String baseAsset, String quoteAsset, String exchange, String interval) {
}
