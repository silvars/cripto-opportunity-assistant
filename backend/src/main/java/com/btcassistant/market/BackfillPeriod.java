package com.btcassistant.market;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/** Converte a configuração de período de backfill (ex. "12-months") em um Instant de início. */
public final class BackfillPeriod {

    private BackfillPeriod() {
    }

    public static Instant resolveStart(String period, Instant now) {
        String[] parts = period.split("-", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Unsupported backfill period: " + period);
        }
        int amount = Integer.parseInt(parts[0]);
        ZonedDateTime reference = now.atZone(ZoneOffset.UTC);
        return switch (parts[1]) {
            case "days" -> reference.minusDays(amount).toInstant();
            case "months" -> reference.minusMonths(amount).toInstant();
            case "years" -> reference.minusYears(amount).toInstant();
            default -> throw new IllegalArgumentException("Unsupported backfill period unit: " + parts[1]);
        };
    }
}
