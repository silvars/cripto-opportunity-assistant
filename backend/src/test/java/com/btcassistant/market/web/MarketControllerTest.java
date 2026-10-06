package com.btcassistant.market.web;

import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleEntity;
import com.btcassistant.market.CandleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketControllerTest {

    @Mock
    private CandleRepository candleRepository;

    private static Candle candle(Instant openTime) {
        return new Candle("BTCUSDT", "1m", openTime, openTime.plusSeconds(59),
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"),
                new BigDecimal("10"), new BigDecimal("1000"), 50, "BINANCE_REST");
    }

    @Test
    void returnsMappedCandlesInRange() {
        MarketController controller = new MarketController(candleRepository);
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = from.plusSeconds(120);
        when(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc("BTCUSDT", "1m", from, to))
                .thenReturn(List.of(CandleEntity.from(candle(from))));

        List<CandleResponse> responses = controller.candles("BTCUSDT", "1m", from, to);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).symbol()).isEqualTo("BTCUSDT");
        assertThat(responses.get(0).close()).isEqualByComparingTo(new BigDecimal("105"));
    }

    @Test
    void returnsNullWhenNoLatestCandle() {
        MarketController controller = new MarketController(candleRepository);
        when(candleRepository.findTopBySymbolAndIntervalOrderByOpenTimeDesc("BTCUSDT", "1m")).thenReturn(Optional.empty());

        assertThat(controller.latest("BTCUSDT", "1m")).isNull();
    }
}
