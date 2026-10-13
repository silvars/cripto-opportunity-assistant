package com.btcassistant.event;

import com.btcassistant.feature.FeatureSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * Snapshots de features estritamente anteriores ao candle de referência (SDD Fase 2 §4) — usada
 * por regras que precisam comparar o estado atual com o estado imediatamente anterior (ex.
 * {@code BREAKOUT}: só dispara quando a máxima é rompida agora, não quando já estava rompida há
 * vários candles).
 */
public record FeatureHistory(List<FeatureSnapshot> recentSnapshotsAscending) {

    /** O snapshot mais recente antes do atual, se existir (histórico insuficiente no início da série). */
    public Optional<FeatureSnapshot> previous() {
        if (recentSnapshotsAscending.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(recentSnapshotsAscending.get(recentSnapshotsAscending.size() - 1));
    }
}
