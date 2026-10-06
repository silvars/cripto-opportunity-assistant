package com.btcassistant.market.web;

import com.btcassistant.market.Instrument;
import com.btcassistant.market.MarketPhase;
import com.btcassistant.market.MarketProperties;
import com.btcassistant.market.MarketStatus;
import com.btcassistant.market.MarketStatusRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketStatusControllerTest {

    @Mock
    private MarketStatusRegistry statusRegistry;
    @Mock
    private MarketProperties marketProperties;

    @Test
    void listsStatusForAllEnabledInstrumentsAndSingleLookup() {
        Instrument btc = new Instrument("BTCUSDT", "BTC", "USDT", "BINANCE", "1m", true);
        when(marketProperties.enabledInstruments()).thenReturn(List.of(btc));
        MarketStatus status = new MarketStatus("BTCUSDT", MarketPhase.LIVE, true, true, 10L,
                Instant.EPOCH, Instant.EPOCH, Instant.EPOCH, Instant.now());
        when(statusRegistry.snapshot("BTCUSDT")).thenReturn(status);

        MarketStatusController controller = new MarketStatusController(statusRegistry, marketProperties);

        assertThat(controller.status()).containsExactly(status);
        assertThat(controller.status("BTCUSDT")).isEqualTo(status);
    }
}
