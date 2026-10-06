package com.btcassistant.market;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifica a cobertura temporal do histórico — nunca assume completo apenas por `count > 0`
 * (SDD Fase 0 §5). Detecta lacunas no início, no meio e no fim do intervalo requerido.
 */
@Component
public class HistoricalCoverageChecker {

    private final CandleRepository candleRepository;

    public HistoricalCoverageChecker(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    public List<HistoricalRange> findMissingRanges(String symbol, String interval, Instant from, Instant to) {
        List<Instant> existing = candleRepository.findOpenTimesInRange(symbol, interval, from, to);
        Duration step = IntervalSupport.toDuration(interval);
        List<HistoricalRange> missing = new ArrayList<>();

        if (existing.isEmpty()) {
            missing.add(new HistoricalRange(from, to));
            return missing;
        }

        if (existing.get(0).isAfter(from)) {
            missing.add(new HistoricalRange(from, existing.get(0)));
        }

        for (int i = 0; i < existing.size() - 1; i++) {
            Instant expectedNext = existing.get(i).plus(step);
            Instant actualNext = existing.get(i + 1);
            if (actualNext.isAfter(expectedNext)) {
                missing.add(new HistoricalRange(expectedNext, actualNext));
            }
        }

        Instant expectedEnd = existing.get(existing.size() - 1).plus(step);
        if (expectedEnd.isBefore(to)) {
            missing.add(new HistoricalRange(expectedEnd, to));
        }

        return missing;
    }
}
