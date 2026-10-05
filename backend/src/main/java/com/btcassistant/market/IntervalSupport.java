package com.btcassistant.market;

import java.time.Duration;

/** Converte o intervalo configurado (ex. "1m", "4h") na Duration equivalente. */
public final class IntervalSupport {

    private IntervalSupport() {
    }

    public static Duration toDuration(String interval) {
        return switch (interval) {
            case "1m" -> Duration.ofMinutes(1);
            case "5m" -> Duration.ofMinutes(5);
            case "15m" -> Duration.ofMinutes(15);
            case "30m" -> Duration.ofMinutes(30);
            case "1h" -> Duration.ofHours(1);
            case "4h" -> Duration.ofHours(4);
            case "12h" -> Duration.ofHours(12);
            case "24h" -> Duration.ofHours(24);
            default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
        };
    }
}
