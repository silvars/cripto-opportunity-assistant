package com.btcassistant.historical;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Os 8 horizontes de avaliação de outcomes (SDD §30). */
public final class HistoricalHorizons {

    public static final Map<String, Duration> ALL = Map.ofEntries(
            Map.entry("5m", Duration.ofMinutes(5)),
            Map.entry("15m", Duration.ofMinutes(15)),
            Map.entry("30m", Duration.ofMinutes(30)),
            Map.entry("1h", Duration.ofHours(1)),
            Map.entry("2h", Duration.ofHours(2)),
            Map.entry("4h", Duration.ofHours(4)),
            Map.entry("12h", Duration.ofHours(12)),
            Map.entry("24h", Duration.ofHours(24)));

    public static final List<String> LABELS = List.copyOf(ALL.keySet());

    private HistoricalHorizons() {
    }
}
