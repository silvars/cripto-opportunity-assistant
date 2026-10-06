package com.btcassistant.market;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CandleValidatorTest {

    private static Candle validCandle() {
        Instant open = Instant.parse("2026-01-01T00:00:00Z");
        return new Candle("BTCUSDT", "1m", open, open.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "BINANCE_REST");
    }

    @Test
    void validCandlePassesValidation() {
        assertThat(CandleValidator.validate(validCandle()).valid()).isTrue();
    }

    @Test
    void rejectsZeroPrice() {
        Candle base = validCandle();
        Candle candle = new Candle(base.symbol(), base.interval(), base.openTime(), base.closeTime(),
                BigDecimal.ZERO, base.high(), base.low(), base.close(), base.volume(), base.quoteVolume(),
                base.tradeCount(), base.source());

        CandleValidator.ValidationResult result = CandleValidator.validate(candle);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("price must be positive");
    }

    @Test
    void rejectsNegativeVolume() {
        Candle base = validCandle();
        Candle candle = new Candle(base.symbol(), base.interval(), base.openTime(), base.closeTime(),
                base.open(), base.high(), base.low(), base.close(), new BigDecimal("-1"), base.quoteVolume(),
                base.tradeCount(), base.source());

        assertThat(CandleValidator.validate(candle).valid()).isFalse();
    }

    @Test
    void rejectsHighBelowLow() {
        Candle base = validCandle();
        Candle candle = new Candle(base.symbol(), base.interval(), base.openTime(), base.closeTime(),
                base.open(), new BigDecimal("80"), new BigDecimal("90"), base.close(), base.volume(),
                base.quoteVolume(), base.tradeCount(), base.source());

        CandleValidator.ValidationResult result = CandleValidator.validate(candle);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("high must be >= low");
    }

    @Test
    void rejectsHighBelowOpenOrClose() {
        Candle base = validCandle();
        Candle candle = new Candle(base.symbol(), base.interval(), base.openTime(), base.closeTime(),
                new BigDecimal("200"), new BigDecimal("150"), base.low(), base.close(), base.volume(),
                base.quoteVolume(), base.tradeCount(), base.source());

        assertThat(CandleValidator.validate(candle).valid()).isFalse();
    }

    @Test
    void rejectsLowAboveOpenOrClose() {
        Candle base = validCandle();
        Candle candle = new Candle(base.symbol(), base.interval(), base.openTime(), base.closeTime(),
                new BigDecimal("50"), base.high(), new BigDecimal("60"), base.close(), base.volume(),
                base.quoteVolume(), base.tradeCount(), base.source());

        assertThat(CandleValidator.validate(candle).valid()).isFalse();
    }

    @Test
    void rejectsCloseTimeNotAfterOpenTime() {
        Candle base = validCandle();
        Candle candle = new Candle(base.symbol(), base.interval(), base.openTime(), base.openTime(),
                base.open(), base.high(), base.low(), base.close(), base.volume(), base.quoteVolume(),
                base.tradeCount(), base.source());

        CandleValidator.ValidationResult result = CandleValidator.validate(candle);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("closeTime must be after openTime");
    }
}
