package com.btcassistant.alert;

/** Ciclo de vida do alerta (SDD §41). Nesta fase, apenas DETECTED→CREATED→ACTIVE→EXPIRED são alcançados em código — EVALUATED depende de mecanismos de avaliação de outcome de fases futuras (Fase 6). */
public enum AlertStatus {
    DETECTED,
    ANALYZING,
    CREATED,
    ACTIVE,
    EXPIRED,
    EVALUATED
}
