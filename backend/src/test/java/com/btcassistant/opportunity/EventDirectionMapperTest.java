package com.btcassistant.opportunity;

import com.btcassistant.event.EventTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventDirectionMapperTest {

    private final EventDirectionMapper mapper = new EventDirectionMapper();

    @Test
    void bullishEventsMapToBuy() {
        assertThat(mapper.map(EventTypes.RAPID_RISE)).isEqualTo(Direction.BUY);
        assertThat(mapper.map(EventTypes.RECOVERY)).isEqualTo(Direction.BUY);
        assertThat(mapper.map(EventTypes.BREAKOUT)).isEqualTo(Direction.BUY);
    }

    @Test
    void bearishEventsMapToSell() {
        assertThat(mapper.map(EventTypes.RAPID_DROP)).isEqualTo(Direction.SELL);
        assertThat(mapper.map(EventTypes.BREAKDOWN)).isEqualTo(Direction.SELL);
        assertThat(mapper.map(EventTypes.FAILED_RECOVERY)).isEqualTo(Direction.SELL);
        assertThat(mapper.map(EventTypes.FAILED_BREAKOUT)).isEqualTo(Direction.SELL);
    }

    @Test
    void contextEventsMapToNeutral() {
        assertThat(mapper.map(EventTypes.VOLUME_SPIKE)).isEqualTo(Direction.NEUTRAL);
        assertThat(mapper.map(EventTypes.VOLATILITY_EXPANSION)).isEqualTo(Direction.NEUTRAL);
        assertThat(mapper.map(EventTypes.VOLATILITY_CONTRACTION)).isEqualTo(Direction.NEUTRAL);
        assertThat(mapper.map(EventTypes.LOCAL_LOW_APPROACH)).isEqualTo(Direction.NEUTRAL);
        assertThat(mapper.map(EventTypes.LOCAL_HIGH_APPROACH)).isEqualTo(Direction.NEUTRAL);
    }
}
