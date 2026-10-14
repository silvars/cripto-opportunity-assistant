package com.btcassistant.operation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** P&L nunca considera apenas preço bruto — sempre desconta fees/slippage (SDD §55). */
class SimplePnlCalculatorTest {

    private final SimplePnlCalculator calculator = new SimplePnlCalculator();
    private static final Instant ENTRY_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private TradeEntity closedTrade() {
        return TradeEntity.open(UUID.randomUUID());
    }

    private OperationEntity operation(Side side, BigDecimal quantity, BigDecimal price, BigDecimal fees,
                                      BigDecimal slippage, Instant occurredAt) {
        return OperationEntity.create(UUID.randomUUID(), UUID.randomUUID(), side, quantity, price, fees, slippage, occurredAt);
    }

    @Test
    void computesProfitForLongTrade() {
        OperationEntity entry = operation(Side.BUY, new BigDecimal("1"), new BigDecimal("50000"),
                new BigDecimal("10"), new BigDecimal("5"), ENTRY_TIME);
        OperationEntity exit = operation(Side.SELL, new BigDecimal("1"), new BigDecimal("51000"),
                new BigDecimal("10"), new BigDecimal("5"), ENTRY_TIME.plus(Duration.ofHours(2)));

        PnlResult result = calculator.calculate(closedTrade(), entry, exit);

        assertThat(result.grossPnl()).isEqualByComparingTo("1000");
        assertThat(result.fees()).isEqualByComparingTo("20");
        assertThat(result.estimatedSlippage()).isEqualByComparingTo("10");
        assertThat(result.netPnl()).isEqualByComparingTo("970");
        assertThat(result.returnPct()).isEqualByComparingTo(new BigDecimal("970").divide(new BigDecimal("50000"), java.math.MathContext.DECIMAL64));
        assertThat(result.holdingTime()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void computesProfitForShortTrade() {
        OperationEntity entry = operation(Side.SELL, new BigDecimal("2"), new BigDecimal("50000"),
                BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME);
        OperationEntity exit = operation(Side.BUY, new BigDecimal("2"), new BigDecimal("48000"),
                BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME.plus(Duration.ofMinutes(30)));

        PnlResult result = calculator.calculate(closedTrade(), entry, exit);

        assertThat(result.grossPnl()).isEqualByComparingTo("4000");
        assertThat(result.netPnl()).isEqualByComparingTo("4000");
    }

    @Test
    void computesLossWhenPriceMovesAgainstLongPosition() {
        OperationEntity entry = operation(Side.BUY, new BigDecimal("1"), new BigDecimal("50000"),
                new BigDecimal("5"), new BigDecimal("5"), ENTRY_TIME);
        OperationEntity exit = operation(Side.SELL, new BigDecimal("1"), new BigDecimal("49000"),
                new BigDecimal("5"), new BigDecimal("5"), ENTRY_TIME.plus(Duration.ofMinutes(10)));

        PnlResult result = calculator.calculate(closedTrade(), entry, exit);

        assertThat(result.grossPnl()).isEqualByComparingTo("-1000");
        assertThat(result.netPnl()).isEqualByComparingTo("-1020");
        assertThat(result.netPnl().signum()).isNegative();
    }

    @Test
    void returnPctIsZeroWhenEntryNotionalIsZero() {
        OperationEntity entry = operation(Side.BUY, BigDecimal.ZERO, new BigDecimal("50000"),
                BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME);
        OperationEntity exit = operation(Side.SELL, BigDecimal.ZERO, new BigDecimal("51000"),
                BigDecimal.ZERO, BigDecimal.ZERO, ENTRY_TIME.plus(Duration.ofMinutes(5)));

        PnlResult result = calculator.calculate(closedTrade(), entry, exit);

        assertThat(result.returnPct()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
