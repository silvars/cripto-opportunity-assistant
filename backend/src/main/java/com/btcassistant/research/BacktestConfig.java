package com.btcassistant.research;

import java.time.Instant;

/**
 * As versões (engine/feature/config/historical/target/stop) são informativas, não parâmetros que
 * trocam o comportamento dos engines em tempo de execução — eles são beans singleton configurados
 * uma única vez (SDD ADR implícito desta fase). {@link BacktestRunner} valida que o solicitado bate
 * com o que está realmente configurado, rejeitando a execução caso contrário — nunca roda "como se"
 * fosse outra versão (ver docs/phases/FASE-7-research.md §11).
 */
public record BacktestConfig(
    String symbol,
    Instant from,
    Instant to,
    String engineVersion,
    String featureVersion,
    String configVersion,
    String historicalVersion,
    String targetDefinitionVersion,
    String stopDefinitionVersion,
    boolean outOfSample
) {
}
