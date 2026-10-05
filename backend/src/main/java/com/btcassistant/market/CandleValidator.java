package com.btcassistant.market;

import java.math.BigDecimal;

/** Validação de candle (SDD §17) — candle inválido nunca entra silenciosamente no banco. */
public final class CandleValidator {

    private CandleValidator() {
    }

    public static ValidationResult validate(Candle candle) {
        if (isNotPositive(candle.open()) || isNotPositive(candle.high())
                || isNotPositive(candle.low()) || isNotPositive(candle.close())) {
            return ValidationResult.invalid("price must be positive");
        }
        if (candle.volume().compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.invalid("volume must not be negative");
        }
        if (candle.high().compareTo(candle.low()) < 0) {
            return ValidationResult.invalid("high must be >= low");
        }
        if (candle.high().compareTo(candle.open()) < 0 || candle.high().compareTo(candle.close()) < 0) {
            return ValidationResult.invalid("high must be >= open and close");
        }
        if (candle.low().compareTo(candle.open()) > 0 || candle.low().compareTo(candle.close()) > 0) {
            return ValidationResult.invalid("low must be <= open and close");
        }
        if (!candle.closeTime().isAfter(candle.openTime())) {
            return ValidationResult.invalid("closeTime must be after openTime");
        }
        return ValidationResult.ok();
    }

    private static boolean isNotPositive(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }

    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
