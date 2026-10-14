package com.btcassistant.decision;

/** Decisão do usuário sobre um alerta (SDD §52). Uma decisão não implica operação. */
public enum Decision {
    BUY,
    SELL,
    IGNORE,
    WAIT
}
